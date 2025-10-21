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
import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunSeriesRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastSnapshotRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ForecastExecutionServiceImpl implements ForecastExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ForecastExecutionServiceImpl.class);

    private final RegionRepository regionRepository;
    private final CropRepository cropRepository;
    private final ForecastModelRepository forecastModelRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final ForecastRunRepository forecastRunRepository;
    private final ForecastRunSeriesRepository forecastRunSeriesRepository;
    private final ForecastSnapshotRepository forecastSnapshotRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final ForecastEngineClient forecastEngineClient;

    public ForecastExecutionServiceImpl(RegionRepository regionRepository,
                                        CropRepository cropRepository,
                                        ForecastModelRepository forecastModelRepository,
                                        YieldRecordRepository yieldRecordRepository,
                                        ForecastRunRepository forecastRunRepository,
                                        ForecastRunSeriesRepository forecastRunSeriesRepository,
                                        ForecastSnapshotRepository forecastSnapshotRepository,
                                        ForecastTaskRepository forecastTaskRepository,
                                        ForecastResultRepository forecastResultRepository,
                                        ForecastEngineClient forecastEngineClient) {
        this.regionRepository = regionRepository;
        this.cropRepository = cropRepository;
        this.forecastModelRepository = forecastModelRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.forecastRunRepository = forecastRunRepository;
        this.forecastRunSeriesRepository = forecastRunSeriesRepository;
        this.forecastSnapshotRepository = forecastSnapshotRepository;
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
        MeasurementType measurementType = resolveMeasurementType(historyRecords);
        List<HistoryObservation> usableHistory = historyRecords.stream()
            .map(record -> mapObservation(record, measurementType))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (usableHistory.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少历史产量数据，无法生成预测");
        }

        int historyLimit = request.historyYears() != null
            ? request.historyYears()
            : Math.min(usableHistory.size(), 10);
        List<HistoryObservation> limitedHistory = usableHistory.stream()
            .sorted(Comparator.comparingInt(obs -> obs.record().getYear()))
            .skip(Math.max(usableHistory.size() - historyLimit, 0))
            .collect(Collectors.toList());

        int requestedForecastPeriods = request.forecastPeriods() != null ? request.forecastPeriods() : 3;
        int forecastPeriods = Math.max(1, Math.min(requestedForecastPeriods, 3));

        ForecastRun run = new ForecastRun();
        run.setRegion(region);
        run.setCrop(crop);
        run.setModel(model);
        run.setForecastPeriods(forecastPeriods);
        run.setHistoryYears(historyLimit);
        run.setFrequency(request.frequency() != null ? request.frequency() : "YEARLY");
        run.setStatus(ForecastRun.RunStatus.RUNNING);
        run.setMeasurementLabel(measurementType.valueLabel());
        run.setMeasurementUnit(measurementType.valueUnit());
        forecastRunRepository.save(run);

        ForecastEngineResponse response = invokeEngine(limitedHistory, run);

        run.setStatus(ForecastRun.RunStatus.SUCCESS);
        run.setExternalRequestId(response.requestId());
        if (response.metrics() != null) {
            run.setMae(response.metrics().mae());
            run.setRmse(response.metrics().rmse());
            run.setMape(response.metrics().mape());
            run.setR2(response.metrics().r2());
        }
        forecastRunRepository.save(run);

        List<ForecastRunSeries> series = new ArrayList<>();
        for (HistoryObservation observation : limitedHistory) {
            ForecastRunSeries item = new ForecastRunSeries();
            item.setRun(run);
            item.setPeriod(String.valueOf(observation.record().getYear()));
            item.setValue(observation.value());
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

        YieldRecord referenceRecord = limitedHistory.isEmpty() ? null : limitedHistory.get(limitedHistory.size() - 1).record();
        List<ForecastSnapshot> snapshots = response.forecast().stream()
            .map(point -> buildSnapshot(run, point, measurementType, referenceRecord))
            .toList();
        Long primaryResultId = null;
        if (!snapshots.isEmpty()) {
            try {
                forecastSnapshotRepository.saveAll(snapshots);
            } catch (DataAccessException ex) {
                log.warn("Failed to persist forecast snapshots for run {}", run.getId(), ex);
            }
            primaryResultId = persistForecastResults(run, snapshots);
        }

        List<ForecastExecutionResponse.SeriesPoint> history = limitedHistory.stream()
            .map(observation -> new ForecastExecutionResponse.SeriesPoint(
                String.valueOf(observation.record().getYear()),
                observation.value(),
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
            run.getUpdatedAt(),
            run.getMeasurementLabel(),
            run.getMeasurementUnit()
        );
        ForecastExecutionResponse.EvaluationMetrics metrics = new ForecastExecutionResponse.EvaluationMetrics(
            run.getMae(),
            run.getRmse(),
            run.getMape(),
            run.getR2()
        );

        return new ForecastExecutionResponse(run.getId(), metadata, history, forecast, metrics, primaryResultId);
    }

    private Long persistForecastResults(ForecastRun run,
                                        List<ForecastSnapshot> snapshots) {
        ForecastTask task = resolveForecastTask(run);
        if (task == null) {
            return null;
        }
        String evaluation = buildEvaluationSummary(run);
        Long primaryResultId = null;
        for (ForecastSnapshot snapshot : snapshots) {
            Integer targetYear = snapshot.getYear();
            if (targetYear == null) {
                continue;
            }
            Double predictedYield = snapshot.getPredictedYield();
            if (predictedYield == null) {
                predictedYield = deriveYieldFromSnapshot(snapshot);
            }
            if (predictedYield == null) {
                continue;
            }
            ForecastResult result = forecastResultRepository
                .findByTaskIdAndTargetYear(task.getId(), targetYear)
                .orElseGet(ForecastResult::new);
            result.setTask(task);
            result.setTargetYear(targetYear);
            result.setPredictedYield(predictedYield);
            result.setEvaluation(evaluation);
            ForecastResult saved = forecastResultRepository.save(result);
            if (primaryResultId == null) {
                primaryResultId = saved.getId();
            }
        }
        return primaryResultId;
    }

    private ForecastTask resolveForecastTask(ForecastRun run) {
        return forecastTaskRepository.findByModelIdAndCropIdAndRegionId(
                run.getModel().getId(),
                run.getCrop().getId(),
                run.getRegion().getId()
            )
            .map(existing -> updateTaskFromRun(existing, run))
            .orElseGet(() -> createTaskFromRun(run));
    }

    private ForecastTask updateTaskFromRun(ForecastTask task, ForecastRun run) {
        task.setStatus(ForecastTask.TaskStatus.SUCCESS);
        task.setParameters(buildTaskParameters(run));
        return forecastTaskRepository.save(task);
    }

    private ForecastTask createTaskFromRun(ForecastRun run) {
        ForecastTask task = new ForecastTask();
        task.setModel(run.getModel());
        task.setCrop(run.getCrop());
        task.setRegion(run.getRegion());
        task.setStatus(ForecastTask.TaskStatus.SUCCESS);
        task.setParameters(buildTaskParameters(run));
        return forecastTaskRepository.save(task);
    }

    private String buildTaskParameters(ForecastRun run) {
        List<String> parameters = new ArrayList<>();
        if (run.getHistoryYears() != null) {
            parameters.add("historyYears=" + run.getHistoryYears());
        }
        if (run.getForecastPeriods() != null) {
            parameters.add("forecastPeriods=" + run.getForecastPeriods());
        }
        if (run.getFrequency() != null) {
            parameters.add("frequency=" + run.getFrequency());
        }
        return parameters.isEmpty() ? null : String.join("; ", parameters);
    }

    private String buildEvaluationSummary(ForecastRun run) {
        List<String> parts = new ArrayList<>();
        if (run.getMae() != null) {
            parts.add("MAE=" + formatMetricValue(run.getMae()));
        }
        if (run.getRmse() != null) {
            parts.add("RMSE=" + formatMetricValue(run.getRmse()));
        }
        if (run.getMape() != null) {
            parts.add("MAPE=" + formatMetricValue(run.getMape()) + "%");
        }
        if (run.getR2() != null) {
            parts.add("R²=" + formatMetricValue(run.getR2()));
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private String formatMetricValue(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private Double deriveYieldFromSnapshot(ForecastSnapshot snapshot) {
        Double predictedProduction = snapshot.getPredictedProduction();
        Double sownArea = snapshot.getSownArea();
        if (predictedProduction != null && sownArea != null && sownArea != 0d) {
            return predictedProduction / sownArea;
        }
        return null;
    }

    private ForecastSnapshot buildSnapshot(ForecastRun run,
                                           ForecastEngineResponse.ForecastPoint point,
                                           MeasurementType measurementType,
                                           YieldRecord referenceRecord) {
        ForecastSnapshot snapshot = new ForecastSnapshot();
        snapshot.setRun(run);
        snapshot.setPeriod(point.period());
        snapshot.setYear(parseYear(point.period()));
        snapshot.setMeasurementValue(point.value());
        snapshot.setMeasurementLabel(measurementType.valueLabel());
        snapshot.setMeasurementUnit(measurementType.valueUnit());

        Double referenceArea = referenceRecord != null ? referenceRecord.getSownArea() : null;
        Double referencePrice = referenceRecord != null ? referenceRecord.getAveragePrice() : null;

        Double predictedProduction = null;
        Double predictedYield = null;

        if (measurementType == MeasurementType.PRODUCTION) {
            predictedProduction = point.value();
            if (referenceArea != null && referenceArea != 0d) {
                predictedYield = point.value() / referenceArea;
            }
        } else {
            predictedYield = point.value();
            if (referenceArea != null) {
                predictedProduction = point.value() * referenceArea;
            }
        }

        snapshot.setPredictedProduction(predictedProduction);
        snapshot.setPredictedYield(predictedYield);
        snapshot.setSownArea(referenceArea);
        snapshot.setAveragePrice(referencePrice);
        snapshot.setEstimatedRevenue(calculateRevenue(predictedProduction, referencePrice));
        return snapshot;
    }

    private Double calculateRevenue(Double production, Double averagePrice) {
        if (production == null || averagePrice == null) {
            return null;
        }
        return production * averagePrice * 0.1d;
    }

    private Integer parseYear(String period) {
        if (period == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : period.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ForecastEngineResponse invokeEngine(List<HistoryObservation> history,
                                                ForecastRun run) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("historyYears", run.getHistoryYears());
        parameters.put("frequency", run.getFrequency());

        List<ForecastEngineRequest.HistoryPoint> historyPoints = history.stream()
            .map(item -> new ForecastEngineRequest.HistoryPoint(
                String.valueOf(item.record().getYear()),
                item.value()
            ))
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
            throw new BusinessException(ResultCode.SERVER_ERROR, "预测引擎调用失败: " + ex.getMessage());
        }
    }

    private MeasurementType resolveMeasurementType(List<YieldRecord> records) {
        boolean yieldAvailable = records.stream().anyMatch(this::hasYieldMeasurement);
        if (yieldAvailable) {
            return MeasurementType.YIELD_PER_HECTARE;
        }
        boolean productionAvailable = records.stream().anyMatch(record -> record.getProduction() != null);
        if (productionAvailable) {
            return MeasurementType.PRODUCTION;
        }
        return MeasurementType.YIELD_PER_HECTARE;
    }

    private HistoryObservation mapObservation(YieldRecord record, MeasurementType measurementType) {
        return switch (measurementType) {
            case YIELD_PER_HECTARE -> {
                Double value = resolveYield(record);
                yield value != null ? new HistoryObservation(record, value) : null;
            }
            case PRODUCTION -> {
                Double production = record.getProduction();
                yield production != null ? new HistoryObservation(record, production) : null;
            }
        };
    }

    private boolean hasYieldMeasurement(YieldRecord record) {
        if (record.getYieldPerHectare() != null) {
            return true;
        }
        Double production = record.getProduction();
        Double sownArea = record.getSownArea();
        return production != null && sownArea != null && sownArea != 0d;
    }

    private Double resolveYield(YieldRecord record) {
        if (record.getYieldPerHectare() != null) {
            return record.getYieldPerHectare();
        }
        Double production = record.getProduction();
        Double sownArea = record.getSownArea();
        if (production != null && sownArea != null && sownArea != 0d) {
            return production / sownArea;
        }
        return null;
    }

    private enum MeasurementType {
        YIELD_PER_HECTARE("单位面积产量", "吨 / 公顷"),
        PRODUCTION("总产量", "吨");

        private final String valueLabel;
        private final String valueUnit;

        MeasurementType(String valueLabel, String valueUnit) {
            this.valueLabel = valueLabel;
            this.valueUnit = valueUnit;
        }

        public String valueLabel() {
            return valueLabel;
        }

        public String valueUnit() {
            return valueUnit;
        }
    }

    private record HistoryObservation(YieldRecord record, double value) { }
}
