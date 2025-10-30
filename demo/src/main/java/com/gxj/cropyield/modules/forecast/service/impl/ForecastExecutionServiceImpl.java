package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.enums.HarvestSeason;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.entity.WeatherRecord;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.WeatherRecordRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
/**
 * 预测管理模块的业务实现类，负责落实预测管理领域的业务处理逻辑。
 * <p>核心方法：runForecast、persistForecastResults、resolveForecastTask、updateTaskFromRun、createTaskFromRun、buildTaskParameters、buildEvaluationSummary、formatMetricValue。</p>
 */

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
    private final WeatherRecordRepository weatherRecordRepository;

    public ForecastExecutionServiceImpl(RegionRepository regionRepository,
                                        CropRepository cropRepository,
                                        ForecastModelRepository forecastModelRepository,
                                        YieldRecordRepository yieldRecordRepository,
                                        ForecastRunRepository forecastRunRepository,
                                        ForecastRunSeriesRepository forecastRunSeriesRepository,
                                        ForecastSnapshotRepository forecastSnapshotRepository,
                                        ForecastTaskRepository forecastTaskRepository,
                                        ForecastResultRepository forecastResultRepository,
                                        ForecastEngineClient forecastEngineClient,
                                        WeatherRecordRepository weatherRecordRepository) {
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
        this.weatherRecordRepository = weatherRecordRepository;
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
            .toList();

        if (usableHistory.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少历史产量数据，无法生成预测");
        }

        int historyLimit = request.historyYears() != null
            ? request.historyYears()
            : Math.min(usableHistory.size(), 10);
        List<HistoryObservation> limitedHistory = usableHistory.stream()
            .sorted(Comparator.comparingInt(obs -> obs.record().getYear()))
            .skip(Math.max(usableHistory.size() - historyLimit, 0))
            .toList();

        Map<Integer, Map<String, Double>> weatherFeatures = Collections.emptyMap();
        if (model.getType() == ForecastModel.ModelType.WEATHER_REGRESSION) {
            Map<Integer, Map<String, Double>> computedFeatures = buildWeatherFeatures(region.getId(), crop, limitedHistory);
            long coveredYears = limitedHistory.stream()
                .map(observation -> observation.record().getYear())
                .filter(year -> computedFeatures.containsKey(year))
                .distinct()
                .count();
            if (coveredYears < 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "所选地区缺少至少两年的天气数据，无法生成预测");
            }
            weatherFeatures = computedFeatures;
        }

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

        ForecastEngineResponse response = invokeEngine(limitedHistory, run, weatherFeatures);

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
            Double measurementValue = snapshot.getMeasurementValue();
            Double predictedYield = snapshot.getPredictedYield();
            if (predictedYield == null) {
                predictedYield = deriveYieldFromSnapshot(snapshot);
            }
            Double predictedProduction = snapshot.getPredictedProduction();
            if (measurementValue == null && predictedYield == null && predictedProduction == null) {
                continue;
            }
            ForecastResult result = forecastResultRepository
                .findByTaskIdAndTargetYear(task.getId(), targetYear)
                .orElseGet(ForecastResult::new);
            result.setTask(task);
            result.setTargetYear(targetYear);
            result.setPredictedYield(predictedYield);
            result.setPredictedProduction(predictedProduction);
            result.setMeasurementValue(measurementValue != null ? measurementValue : predictedYield);
            result.setMeasurementLabel(snapshot.getMeasurementLabel());
            result.setMeasurementUnit(snapshot.getMeasurementUnit());
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

    private Map<Integer, Map<String, Double>> buildWeatherFeatures(Long regionId,
                                                                   Crop crop,
                                                                   List<HistoryObservation> history) {
        if (history.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Integer> historyYears = new LinkedHashSet<>();
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        for (HistoryObservation observation : history) {
            int year = observation.record().getYear();
            historyYears.add(year);
            if (year < minYear) {
                minYear = year;
            }
            if (year > maxYear) {
                maxYear = year;
            }
        }
        if (historyYears.isEmpty()) {
            return Collections.emptyMap();
        }

        HarvestSeason season = crop != null && crop.getHarvestSeason() != null
            ? crop.getHarvestSeason()
            : HarvestSeason.ANNUAL;
        SeasonalDefinition definition = SeasonalDefinition.of(season);

        LocalDate queryStart = definition.computeQueryStart(minYear);
        LocalDate queryEnd = definition.computeQueryEnd(maxYear);
        List<WeatherRecord> records = weatherRecordRepository
            .findByRegionIdAndRecordDateBetween(regionId, queryStart, queryEnd);
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Map<String, WeatherStats>> statsByYear = new HashMap<>();
        for (Integer year : historyYears) {
            Map<String, WeatherStats> windows = new LinkedHashMap<>();
            windows.put("season", new WeatherStats());
            for (String prefix : definition.windowPrefixes()) {
                windows.put(prefix, new WeatherStats());
            }
            statsByYear.put(year, windows);
        }

        for (WeatherRecord record : records) {
            LocalDate date = record.getRecordDate();
            if (date == null) {
                continue;
            }
            Integer targetYear = definition.resolveTargetYear(date);
            if (targetYear == null || !historyYears.contains(targetYear)) {
                continue;
            }
            Map<String, WeatherStats> windows = statsByYear.get(targetYear);
            if (windows == null) {
                continue;
            }
            WeatherStats seasonStats = windows.get("season");
            if (seasonStats == null) {
                seasonStats = new WeatherStats();
                windows.put("season", seasonStats);
            }
            seasonStats.accept(record);
            String windowKey = definition.resolveWindowKey(date);
            if (windowKey != null) {
                windows.computeIfAbsent(windowKey, key -> new WeatherStats()).accept(record);
            }
        }

        Map<Integer, Map<String, Double>> prepared = new HashMap<>();
        Set<String> commonKeys = null;
        for (Map.Entry<Integer, Map<String, WeatherStats>> entry : statsByYear.entrySet()) {
            Map<String, Double> featureMap = new LinkedHashMap<>();
            for (Map.Entry<String, WeatherStats> windowEntry : entry.getValue().entrySet()) {
                appendWeatherFeatures(featureMap, windowEntry.getKey(), windowEntry.getValue());
            }
            if (!featureMap.isEmpty()) {
                prepared.put(entry.getKey(), featureMap);
                if (commonKeys == null) {
                    commonKeys = new LinkedHashSet<>(featureMap.keySet());
                } else {
                    commonKeys.retainAll(featureMap.keySet());
                }
            }
        }

        if (prepared.isEmpty() || commonKeys == null || commonKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Map<String, Double>> aligned = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Double>> entry : prepared.entrySet()) {
            Map<String, Double> filtered = new LinkedHashMap<>();
            boolean complete = true;
            for (String key : commonKeys) {
                Double value = entry.getValue().get(key);
                if (value == null) {
                    complete = false;
                    break;
                }
                filtered.put(key, value);
            }
            if (complete) {
                aligned.put(entry.getKey(), filtered);
            }
        }
        return aligned;
    }

    private void appendWeatherFeatures(Map<String, Double> featureMap,
                                       String prefix,
                                       WeatherStats stats) {
        if (stats == null) {
            return;
        }
        String effectivePrefix = "season".equals(prefix) ? "" : prefix;
        stats.averageMax()
            .ifPresent(value -> featureMap.put(composeFeatureName(effectivePrefix, "AvgMaxTemperature"), roundFeature(value)));
        stats.averageMin()
            .ifPresent(value -> featureMap.put(composeFeatureName(effectivePrefix, "AvgMinTemperature"), roundFeature(value)));
        stats.temperatureRange()
            .ifPresent(value -> featureMap.put(composeFeatureName(effectivePrefix, "AvgDiurnalRange"), roundFeature(value)));
        stats.totalSunshine()
            .ifPresent(value -> featureMap.put(composeFeatureName(effectivePrefix, "TotalSunshineHours"), roundFeature(value)));
    }

    private String composeFeatureName(String prefix, String suffix) {
        if (prefix == null || prefix.isEmpty()) {
            return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
        }
        return prefix + suffix;
    }

    private double roundFeature(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    private static final class WeatherStats {
        private double maxSum;
        private int maxCount;
        private double minSum;
        private int minCount;
        private double sunshineSum;
        private int sunshineCount;
        private double rangeSum;
        private int rangeCount;

        void accept(WeatherRecord record) {
            if (record.getMaxTemperature() != null) {
                maxSum += record.getMaxTemperature();
                maxCount++;
            }
            if (record.getMinTemperature() != null) {
                minSum += record.getMinTemperature();
                minCount++;
            }
            if (record.getSunshineHours() != null) {
                sunshineSum += record.getSunshineHours();
                sunshineCount++;
            }
            if (record.getMaxTemperature() != null && record.getMinTemperature() != null) {
                rangeSum += record.getMaxTemperature() - record.getMinTemperature();
                rangeCount++;
            }
        }

        OptionalDouble averageMax() {
            return maxCount > 0 ? OptionalDouble.of(maxSum / maxCount) : OptionalDouble.empty();
        }

        OptionalDouble averageMin() {
            return minCount > 0 ? OptionalDouble.of(minSum / minCount) : OptionalDouble.empty();
        }

        OptionalDouble totalSunshine() {
            return sunshineCount > 0 ? OptionalDouble.of(sunshineSum) : OptionalDouble.empty();
        }

        OptionalDouble temperatureRange() {
            return rangeCount > 0 ? OptionalDouble.of(rangeSum / rangeCount) : OptionalDouble.empty();
        }
    }

    private static final class SeasonalDefinition {
        private final HarvestSeason season;
        private final List<String> windowPrefixes;

        private SeasonalDefinition(HarvestSeason season, List<String> windowPrefixes) {
            this.season = season;
            this.windowPrefixes = windowPrefixes;
        }

        static SeasonalDefinition of(HarvestSeason season) {
            HarvestSeason resolved = season != null ? season : HarvestSeason.ANNUAL;
            return switch (resolved) {
                case SUMMER_GRAIN -> new SeasonalDefinition(resolved, List.of("winterDormancy", "springRipening"));
                case AUTUMN_GRAIN -> new SeasonalDefinition(resolved, List.of("sowingEstablishment", "grainFill"));
                default -> new SeasonalDefinition(resolved, List.of());
            };
        }

        LocalDate computeQueryStart(int year) {
            return switch (season) {
                case SUMMER_GRAIN -> LocalDate.of(year - 1, 10, 1);
                case AUTUMN_GRAIN -> LocalDate.of(year, 3, 1);
                default -> LocalDate.of(year, 1, 1);
            };
        }

        LocalDate computeQueryEnd(int year) {
            return switch (season) {
                case SUMMER_GRAIN -> LocalDate.of(year, 7, 31);
                case AUTUMN_GRAIN -> LocalDate.of(year, 11, 30);
                default -> LocalDate.of(year, 12, 31);
            };
        }

        Integer resolveTargetYear(LocalDate date) {
            int month = date.getMonthValue();
            return switch (season) {
                case SUMMER_GRAIN -> {
                    if (month >= 10) {
                        yield date.getYear() + 1;
                    }
                    if (month <= 7) {
                        yield date.getYear();
                    }
                    yield null;
                }
                case AUTUMN_GRAIN -> {
                    if (month >= 3 && month <= 11) {
                        yield date.getYear();
                    }
                    yield null;
                }
                default -> date.getYear();
            };
        }

        String resolveWindowKey(LocalDate date) {
            int month = date.getMonthValue();
            return switch (season) {
                case SUMMER_GRAIN -> {
                    if (month >= 10 || month <= 2) {
                        yield "winterDormancy";
                    }
                    if (month >= 3 && month <= 7) {
                        yield "springRipening";
                    }
                    yield null;
                }
                case AUTUMN_GRAIN -> {
                    if (month >= 3 && month <= 6) {
                        yield "sowingEstablishment";
                    }
                    if (month >= 7 && month <= 11) {
                        yield "grainFill";
                    }
                    yield null;
                }
                default -> null;
            };
        }

        List<String> windowPrefixes() {
            return windowPrefixes;
        }
    }

    private ForecastEngineResponse invokeEngine(List<HistoryObservation> history,
                                                ForecastRun run,
                                                Map<Integer, Map<String, Double>> weatherFeatures) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("historyYears", run.getHistoryYears());
        parameters.put("frequency", run.getFrequency());

        List<ForecastEngineRequest.HistoryPoint> historyPoints = history.stream()
            .map(item -> {
                Map<String, Double> features = weatherFeatures.get(item.record().getYear());
                return new ForecastEngineRequest.HistoryPoint(
                    String.valueOf(item.record().getYear()),
                    item.value(),
                    features != null ? new LinkedHashMap<>(features) : null
                );
            })
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
