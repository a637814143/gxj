package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionResponse;
import com.gxj.cropyield.modules.forecast.engine.ForecastEngineClient;
import com.gxj.cropyield.modules.forecast.engine.ForecastEngineRequest;
import com.gxj.cropyield.modules.forecast.engine.ForecastEngineResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import com.gxj.cropyield.modules.forecast.entity.ForecastRunSeries;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunSeriesRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ForecastExecutionServiceImpl implements ForecastExecutionService {

    private final RegionRepository regionRepository;
    private final CropRepository cropRepository;
    private final ForecastModelRepository forecastModelRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final ForecastRunRepository forecastRunRepository;
    private final ForecastRunSeriesRepository forecastRunSeriesRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final ForecastEngineClient forecastEngineClient;

    public ForecastExecutionServiceImpl(RegionRepository regionRepository,
                                        CropRepository cropRepository,
                                        ForecastModelRepository forecastModelRepository,
                                        YieldRecordRepository yieldRecordRepository,
                                        ForecastRunRepository forecastRunRepository,
                                        ForecastRunSeriesRepository forecastRunSeriesRepository,
                                        ForecastTaskRepository forecastTaskRepository,
                                        ForecastResultRepository forecastResultRepository,
                                        ForecastEngineClient forecastEngineClient) {
        this.regionRepository = regionRepository;
        this.cropRepository = cropRepository;
        this.forecastModelRepository = forecastModelRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.forecastRunRepository = forecastRunRepository;
        this.forecastRunSeriesRepository = forecastRunSeriesRepository;
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.forecastEngineClient = forecastEngineClient;
    }

    @Override
    @Transactional
    public ForecastExecutionResponse runForecast(ForecastExecutionRequest request) {
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));
        Crop crop = cropRepository.findById(request.cropId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "作物不存在"));
        ForecastModel model = forecastModelRepository.findById(request.modelId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "模型不存在"));

        List<YieldRecord> historyRecords = yieldRecordRepository
            .findByRegionIdAndCropIdOrderByYearAsc(region.getId(), crop.getId());
        if (historyRecords.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少历史产量数据，无法生成预测");
        }

        int historyLimit = request.historyYears() != null ? request.historyYears() : Math.min(historyRecords.size(), 10);
        int forecastPeriods = request.forecastPeriods() != null ? request.forecastPeriods() : 3;
        String frequency = request.frequency() != null ? request.frequency() : "YEARLY";

        ForecastTask task = forecastTaskRepository
            .findByModelIdAndCropIdAndRegionId(model.getId(), crop.getId(), region.getId())
            .orElseGet(() -> {
                ForecastTask created = new ForecastTask();
                created.setModel(model);
                created.setCrop(crop);
                created.setRegion(region);
                return forecastTaskRepository.save(created);
            });
        task.setStatus(ForecastTask.TaskStatus.RUNNING);
        task.setParameters(buildParametersSummary(historyLimit, forecastPeriods, frequency));
        forecastTaskRepository.save(task);

        List<YieldRecord> limitedHistory = historyRecords.stream()
            .sorted(Comparator.comparingInt(YieldRecord::getYear))
            .skip(Math.max(historyRecords.size() - historyLimit, 0))
            .collect(Collectors.toList());

        ForecastRun run = new ForecastRun();
        run.setRegion(region);
        run.setCrop(crop);
        run.setModel(model);
        run.setForecastPeriods(forecastPeriods);
        run.setHistoryYears(historyLimit);
        run.setFrequency(frequency);
        run.setStatus(ForecastRun.RunStatus.RUNNING);
        forecastRunRepository.save(run);

        ForecastEngineResponse response = invokeEngine(limitedHistory, run, task);

        run.setStatus(ForecastRun.RunStatus.SUCCESS);
        run.setExternalRequestId(response.requestId());
        if (response.metrics() != null) {
            run.setMae(response.metrics().mae());
            run.setRmse(response.metrics().rmse());
            run.setMape(response.metrics().mape());
            run.setR2(response.metrics().r2());
        }
        forecastRunRepository.save(run);

        task.setStatus(ForecastTask.TaskStatus.SUCCESS);
        forecastTaskRepository.save(task);

        upsertForecastResults(task, response, run);

        List<ForecastRunSeries> series = new ArrayList<>();
        for (YieldRecord record : limitedHistory) {
            ForecastRunSeries item = new ForecastRunSeries();
            item.setRun(run);
            item.setPeriod(String.valueOf(record.getYear()));
            item.setValue(record.getYieldPerHectare());
            item.setLowerBound(null);
            item.setUpperBound(null);
            item.setHistorical(Boolean.TRUE);
            series.add(item);
        }

        for (ForecastEngineResponse.ForecastPoint point : response.forecast()) {
            ForecastRunSeries item = new ForecastRunSeries();
            item.setRun(run);
            item.setPeriod(point.period());
            item.setValue(point.value());
            item.setLowerBound(point.lowerBound());
            item.setUpperBound(point.upperBound());
            item.setHistorical(Boolean.FALSE);
            series.add(item);
        }
        forecastRunSeriesRepository.saveAll(series);

        List<ForecastExecutionResponse.SeriesPoint> history = limitedHistory.stream()
            .map(record -> new ForecastExecutionResponse.SeriesPoint(
                String.valueOf(record.getYear()),
                record.getYieldPerHectare(),
                null,
                null
            ))
            .toList();
        List<ForecastExecutionResponse.SeriesPoint> forecast = response.forecast().stream()
            .map(point -> new ForecastExecutionResponse.SeriesPoint(
                point.period(),
                point.value(),
                point.lowerBound(),
                point.upperBound()
            ))
            .toList();

        ForecastExecutionResponse.Metadata metadata = new ForecastExecutionResponse.Metadata(
            region.getName(),
            crop.getName(),
            model.getName(),
            model.getType().name(),
            run.getFrequency(),
            run.getForecastPeriods(),
            run.getUpdatedAt()
        );
        ForecastExecutionResponse.EvaluationMetrics metrics = new ForecastExecutionResponse.EvaluationMetrics(
            run.getMae(),
            run.getRmse(),
            run.getMape(),
            run.getR2()
        );

        return new ForecastExecutionResponse(run.getId(), metadata, history, forecast, metrics);
    }

    private ForecastEngineResponse invokeEngine(List<YieldRecord> history,
                                                ForecastRun run,
                                                ForecastTask task) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("historyYears", run.getHistoryYears());
        parameters.put("frequency", run.getFrequency());

        List<ForecastEngineRequest.HistoryPoint> historyPoints = history.stream()
            .map(item -> new ForecastEngineRequest.HistoryPoint(String.valueOf(item.getYear()), item.getYieldPerHectare()))
            .toList();

        ForecastEngineRequest engineRequest = new ForecastEngineRequest(
            run.getModel().getType().name(),
            run.getFrequency(),
            run.getForecastPeriods(),
            historyPoints,
            parameters
        );
        try {
            return forecastEngineClient.runForecast(engineRequest);
        } catch (Exception ex) {
            run.setStatus(ForecastRun.RunStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            forecastRunRepository.save(run);
            task.setStatus(ForecastTask.TaskStatus.FAILED);
            forecastTaskRepository.save(task);
            throw new BusinessException(ResultCode.SERVER_ERROR, "预测引擎调用失败: " + ex.getMessage());
        }
    }

    private String buildParametersSummary(int historyYears, int forecastPeriods, String frequency) {
        StringJoiner joiner = new StringJoiner(";");
        joiner.add("historyYears=" + historyYears);
        joiner.add("forecastPeriods=" + forecastPeriods);
        joiner.add("frequency=" + frequency);
        return joiner.toString();
    }

    private void upsertForecastResults(ForecastTask task,
                                       ForecastEngineResponse response,
                                       ForecastRun run) {
        if (response.forecast() == null || response.forecast().isEmpty()) {
            return;
        }

        Map<Integer, ForecastResult> existing = forecastResultRepository.findByTaskId(task.getId()).stream()
            .collect(Collectors.toMap(ForecastResult::getTargetYear, Function.identity()));

        List<ForecastResult> toSave = new ArrayList<>();
        for (ForecastEngineResponse.ForecastPoint point : response.forecast()) {
            Integer targetYear = parseTargetYear(point.period());
            if (targetYear == null) {
                continue;
            }
            ForecastResult result = existing.get(targetYear);
            if (result == null) {
                result = new ForecastResult();
                result.setTask(task);
                result.setTargetYear(targetYear);
            }
            result.setPredictedYield(point.value());
            result.setEvaluation(buildEvaluationSummary(point, run));
            toSave.add(result);
        }

        if (!toSave.isEmpty()) {
            forecastResultRepository.saveAll(toSave);
        }
    }

    private Integer parseTargetYear(String period) {
        if (period == null) {
            return null;
        }
        try {
            return Integer.parseInt(period);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildEvaluationSummary(ForecastEngineResponse.ForecastPoint point, ForecastRun run) {
        StringJoiner joiner = new StringJoiner("；");
        joiner.add(String.format(Locale.CHINA, "预测值 %.2f 吨", point.value()));
        if (point.lowerBound() != null && point.upperBound() != null) {
            joiner.add(String.format(Locale.CHINA, "区间 %.2f ~ %.2f 吨", point.lowerBound(), point.upperBound()));
        }

        String metrics = buildMetricsSummary(run);
        if (!metrics.isEmpty()) {
            joiner.add(metrics);
        }
        return joiner.toString();
    }

    private String buildMetricsSummary(ForecastRun run) {
        List<String> metrics = new ArrayList<>();
        if (run.getMae() != null) {
            metrics.add(String.format(Locale.CHINA, "MAE %.2f", run.getMae()));
        }
        if (run.getRmse() != null) {
            metrics.add(String.format(Locale.CHINA, "RMSE %.2f", run.getRmse()));
        }
        if (run.getMape() != null) {
            metrics.add(String.format(Locale.CHINA, "MAPE %.2f%%", run.getMape()));
        }
        if (run.getR2() != null) {
            metrics.add(String.format(Locale.CHINA, "R² %.2f", run.getR2()));
        }
        if (metrics.isEmpty()) {
            return "";
        }
        return "评估指标 " + String.join("，", metrics);
    }
}
