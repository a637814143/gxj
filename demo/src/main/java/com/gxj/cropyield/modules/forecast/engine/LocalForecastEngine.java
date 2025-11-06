package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 预测管理模块的业务组件，封装预测管理的算法或执行流程。
 */

@Component
public class LocalForecastEngine {

    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})[-_/]?Q([1-4])$");

    private static final class DoubleExponentialModel {
        private final double alpha;
        private final double beta;
        private final double level;
        private final double trend;
        private final double mae;
        private final double rmse;
        private final double mape;
        private final Double r2;
        private final int comparisons;

        private DoubleExponentialModel(double alpha,
                                       double beta,
                                       double level,
                                       double trend,
                                       double mae,
                                       double rmse,
                                       double mape,
                                       Double r2,
                                       int comparisons) {
            this.alpha = alpha;
            this.beta = beta;
            this.level = level;
            this.trend = trend;
            this.mae = mae;
            this.rmse = rmse;
            this.mape = mape;
            this.r2 = r2;
            this.comparisons = comparisons;
        }
    }

    private final Dl4jLstmForecaster lstmForecaster = new Dl4jLstmForecaster();
    private static final class WeatherRegressionResult {
        private final List<Double> forecast;
        private final ForecastEngineResponse.EvaluationMetrics metrics;

        private WeatherRegressionResult(List<Double> forecast,
                                        ForecastEngineResponse.EvaluationMetrics metrics) {
            this.forecast = forecast;
            this.metrics = metrics;
        }
    }

    private static final class FeatureTrend {
        private final double intercept;
        private final double slope;
        private final Double lastKnown;
        private final int count;

        private FeatureTrend(double intercept, double slope, Double lastKnown, int count) {
            this.intercept = intercept;
            this.slope = slope;
            this.lastKnown = lastKnown;
            this.count = count;
        }
    }

    public ForecastEngineResponse forecast(ForecastEngineRequest request) {
        List<ForecastEngineRequest.HistoryPoint> historyPoints = request.history() != null
            ? request.history()
            : Collections.emptyList();
        if (historyPoints.isEmpty()) {
            return new ForecastEngineResponse(
                generateRequestId(),
                List.of(),
                new ForecastEngineResponse.EvaluationMetrics(null, null, null, null)
            );
        }

        List<ForecastEngineRequest.HistoryPoint> sanitizedHistory = sanitizeHistory(historyPoints);
        ForecastModel.ModelType modelType = resolveModelType(request.modelCode());
        int forecastPeriods = Math.max(1, Math.min(request.forecastPeriods(), 3));

        List<Double> historyValues = sanitizedHistory.stream()
            .map(ForecastEngineRequest.HistoryPoint::value)
            .toList();

        List<Double> rawForecast;
        ForecastEngineResponse.EvaluationMetrics metrics;
        if (modelType == ForecastModel.ModelType.WEATHER_REGRESSION) {
            WeatherRegressionResult regressionResult = weatherRegressionForecast(
                sanitizedHistory,
                historyValues,
                forecastPeriods,
                request.parameters()
            );
            rawForecast = regressionResult.forecast;
            metrics = regressionResult.metrics != null
                ? regressionResult.metrics
                : buildBaselineMetrics(historyValues);
        } else {
            rawForecast = lstmForecaster.forecast(historyValues, forecastPeriods)
                .orElseGet(() -> exponentialSmoothingForecast(historyValues, forecastPeriods));
            metrics = buildBaselineMetrics(historyValues);
        }
        List<String> nextPeriods = buildNextPeriods(sanitizedHistory, request.frequency(), rawForecast.size());

        double confidenceBand = computeConfidenceBand(historyValues);
        List<ForecastEngineResponse.ForecastPoint> points = new ArrayList<>();
        for (int i = 0; i < rawForecast.size(); i++) {
            double value = rawForecast.get(i);
            double lower = Math.max(0d, value - confidenceBand);
            double upper = value + confidenceBand;
            points.add(new ForecastEngineResponse.ForecastPoint(
                nextPeriods.get(i),
                round(value),
                round(lower),
                round(upper)
            ));
        }

        return new ForecastEngineResponse(generateRequestId(), points, metrics);
    }

    private ForecastModel.ModelType resolveModelType(String code) {
        if (code == null) {
            return ForecastModel.ModelType.LSTM;
        }
        try {
            return ForecastModel.ModelType.valueOf(code.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ForecastModel.ModelType.LSTM;
        }
    }

    private List<ForecastEngineRequest.HistoryPoint> sanitizeHistory(List<ForecastEngineRequest.HistoryPoint> history) {
        List<ForecastEngineRequest.HistoryPoint> sanitized = new ArrayList<>();
        Double lastValue = null;
        for (ForecastEngineRequest.HistoryPoint point : history) {
            Double value = point.value() != null ? point.value() : lastValue;
            if (value == null) {
                continue;
            }
            lastValue = value;
            sanitized.add(new ForecastEngineRequest.HistoryPoint(point.period(), value, point.features()));
        }
        return sanitized.isEmpty() ? history : sanitized;
    }

    private List<Double> linearTrendForecast(List<Double> historyValues, int periods) {
        int n = historyValues.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = historyValues.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        List<Double> results = new ArrayList<>();
        for (int i = 1; i <= periods; i++) {
            double x = n + i;
            double forecast = intercept + slope * x;
            if (forecast <= 0) {
                forecast = historyValues.get(n - 1) * 0.9;
            }
            results.add(forecast);
        }
        return results;
    }

    private List<Double> exponentialSmoothingForecast(List<Double> historyValues, int periods) {
        double alpha = 0.6;
        double level = historyValues.get(0);
        double trend = 0;
        for (int i = 1; i < historyValues.size(); i++) {
            double value = historyValues.get(i);
            double prevLevel = level;
            level = alpha * value + (1 - alpha) * (level + trend);
            trend = 0.4 * (level - prevLevel) + (1 - 0.4) * trend;
        }
        List<Double> results = new ArrayList<>();
        for (int i = 1; i <= periods; i++) {
            double forecast = level + trend * i;
            results.add(forecast);
        }
        return results;
    }

    private WeatherRegressionResult weatherRegressionForecast(List<ForecastEngineRequest.HistoryPoint> history,
                                                               List<Double> historyValues,
                                                               int periods,
                                                               Map<String, Object> parameters) {
        List<ForecastEngineRequest.HistoryPoint> usable = new ArrayList<>();
        List<Integer> years = new ArrayList<>();
        for (ForecastEngineRequest.HistoryPoint point : history) {
            if (point.value() == null || point.features() == null || point.features().isEmpty()) {
                continue;
            }
            Optional<Integer> year = parseYear(point.period());
            if (year.isEmpty()) {
                continue;
            }
            usable.add(point);
            years.add(year.get());
        }

        if (usable.size() < 2) {
            return new WeatherRegressionResult(
                linearTrendForecast(historyValues, periods),
                buildBaselineMetrics(historyValues)
            );
        }

        Set<String> featureNames = new TreeSet<>();
        for (ForecastEngineRequest.HistoryPoint point : usable) {
            featureNames.addAll(point.features().keySet());
        }
        if (featureNames.isEmpty()) {
            return new WeatherRegressionResult(
                linearTrendForecast(historyValues, periods),
                buildBaselineMetrics(historyValues)
            );
        }

        List<String> featureKeys = new ArrayList<>(featureNames);
        Map<String, Double> featureMeans = new HashMap<>();
        for (String key : featureKeys) {
            double sum = 0d;
            int count = 0;
            for (ForecastEngineRequest.HistoryPoint point : usable) {
                Double value = point.features().get(key);
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            featureMeans.put(key, count > 0 ? sum / count : 0d);
        }

        int samples = usable.size();
        int columns = featureKeys.size() + 1;
        double[][] design = new double[samples][columns];
        double[] outputs = new double[samples];
        for (int i = 0; i < samples; i++) {
            design[i][0] = 1d;
            ForecastEngineRequest.HistoryPoint point = usable.get(i);
            outputs[i] = point.value();
            for (int j = 0; j < featureKeys.size(); j++) {
                String key = featureKeys.get(j);
                Double value = point.features().get(key);
                design[i][j + 1] = value != null ? value : featureMeans.getOrDefault(key, 0d);
            }
        }

        double[] coefficients = solveNormalEquation(design, outputs);
        if (coefficients == null) {
            return new WeatherRegressionResult(
                linearTrendForecast(historyValues, periods),
                buildBaselineMetrics(historyValues)
            );
        }

        double[] fitted = new double[samples];
        for (int i = 0; i < samples; i++) {
            double estimate = 0d;
            for (int j = 0; j < coefficients.length; j++) {
                estimate += design[i][j] * coefficients[j];
            }
            fitted[i] = estimate;
        }

        Map<String, FeatureTrend> featureTrends = computeFeatureTrends(usable, years, featureKeys);
        Map<String, Double> lastKnown = new HashMap<>();
        for (ForecastEngineRequest.HistoryPoint point : usable) {
            for (String key : featureKeys) {
                Double value = point.features().get(key);
                if (value != null) {
                    lastKnown.put(key, value);
                }
            }
        }

        int lastYear = years.get(years.size() - 1);
        Map<Integer, Map<String, Double>> providedFutureFeatures = extractFutureWeatherFeatures(parameters);
        List<Double> forecast = new ArrayList<>();
        for (int step = 1; step <= periods; step++) {
            int targetYear = lastYear + step;
            double prediction = coefficients[0];
            Map<String, Double> providedForYear = providedFutureFeatures.get(targetYear);
            for (int j = 0; j < featureKeys.size(); j++) {
                String key = featureKeys.get(j);
                FeatureTrend trend = featureTrends.get(key);
                Double providedValue = providedForYear != null ? providedForYear.get(key) : null;
                double projected;
                if (providedValue != null) {
                    projected = providedValue;
                } else if (trend != null && trend.count >= 2) {
                    projected = trend.intercept + trend.slope * targetYear;
                } else if (trend != null && trend.lastKnown != null) {
                    projected = trend.lastKnown;
                } else {
                    projected = featureMeans.getOrDefault(key, lastKnown.getOrDefault(key, 0d));
                }
                prediction += coefficients[j + 1] * projected;
            }
            if (!historyValues.isEmpty() && prediction <= 0) {
                prediction = Math.max(historyValues.get(historyValues.size() - 1) * 0.9, 0d);
            }
            forecast.add(prediction);
        }

        ForecastEngineResponse.EvaluationMetrics metrics = buildRegressionMetrics(outputs, fitted);
        return new WeatherRegressionResult(forecast, metrics);
    }

    private Map<Integer, Map<String, Double>> extractFutureWeatherFeatures(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }
        Object raw = parameters.get("futureWeatherFeatures");
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            Integer year;
            try {
                year = Integer.parseInt(entry.getKey().toString());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> inner)) {
                continue;
            }
            Map<String, Double> features = new HashMap<>();
            for (Map.Entry<?, ?> featureEntry : inner.entrySet()) {
                if (featureEntry.getKey() == null || featureEntry.getValue() == null) {
                    continue;
                }
                Double value = convertToDouble(featureEntry.getValue());
                if (value != null) {
                    features.put(featureEntry.getKey().toString(), value);
                }
            }
            if (!features.isEmpty()) {
                result.put(year, features);
            }
        }
        return result;
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double[] solveNormalEquation(double[][] designMatrix, double[] outputs) {
        if (designMatrix.length == 0) {
            return null;
        }
        int columns = designMatrix[0].length;
        double[][] xtx = new double[columns][columns];
        double[] xty = new double[columns];
        for (int i = 0; i < designMatrix.length; i++) {
            for (int j = 0; j < columns; j++) {
                xty[j] += designMatrix[i][j] * outputs[i];
                for (int k = 0; k < columns; k++) {
                    xtx[j][k] += designMatrix[i][j] * designMatrix[i][k];
                }
            }
        }
        return gaussianElimination(xtx, xty);
    }

    private double[] gaussianElimination(double[][] matrix, double[] vector) {
        int n = vector.length;
        double[][] augmented = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n] = vector[i];
        }
        for (int pivot = 0; pivot < n; pivot++) {
            int maxRow = pivot;
            double maxValue = Math.abs(augmented[pivot][pivot]);
            for (int row = pivot + 1; row < n; row++) {
                double value = Math.abs(augmented[row][pivot]);
                if (value > maxValue) {
                    maxValue = value;
                    maxRow = row;
                }
            }
            if (maxValue < 1e-9) {
                return null;
            }
            if (maxRow != pivot) {
                double[] temp = augmented[pivot];
                augmented[pivot] = augmented[maxRow];
                augmented[maxRow] = temp;
            }
            double pivotValue = augmented[pivot][pivot];
            for (int col = pivot; col <= n; col++) {
                augmented[pivot][col] /= pivotValue;
            }
            for (int row = 0; row < n; row++) {
                if (row == pivot) {
                    continue;
                }
                double factor = augmented[row][pivot];
                for (int col = pivot; col <= n; col++) {
                    augmented[row][col] -= factor * augmented[pivot][col];
                }
            }
        }
        double[] solution = new double[n];
        for (int i = 0; i < n; i++) {
            solution[i] = augmented[i][n];
        }
        return solution;
    }

    private Map<String, FeatureTrend> computeFeatureTrends(List<ForecastEngineRequest.HistoryPoint> history,
                                                           List<Integer> years,
                                                           List<String> featureKeys) {
        Map<String, FeatureTrend> trends = new HashMap<>();
        for (String key : featureKeys) {
            double sumX = 0d;
            double sumY = 0d;
            double sumXY = 0d;
            double sumXX = 0d;
            int count = 0;
            Double lastKnown = null;
            for (int i = 0; i < history.size(); i++) {
                Double value = history.get(i).features().get(key);
                if (value == null) {
                    continue;
                }
                int year = years.get(i);
                sumX += year;
                sumY += value;
                sumXY += year * value;
                sumXX += year * year;
                lastKnown = value;
                count++;
            }
            if (count >= 2) {
                double denominator = count * sumXX - sumX * sumX;
                double slope = denominator == 0 ? 0 : (count * sumXY - sumX * sumY) / denominator;
                double intercept = (sumY - slope * sumX) / count;
                trends.put(key, new FeatureTrend(intercept, slope, lastKnown, count));
            } else if (count == 1) {
                trends.put(key, new FeatureTrend(0, 0, lastKnown, count));
            } else {
                trends.put(key, new FeatureTrend(0, 0, null, 0));
            }
        }
        return trends;
    }

    private ForecastEngineResponse.EvaluationMetrics buildRegressionMetrics(double[] actual,
                                                                            double[] predicted) {
        if (actual.length == 0) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double sumAbs = 0d;
        double sumSq = 0d;
        double sumPct = 0d;
        double sumActual = 0d;
        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - predicted[i];
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (Math.abs(actual[i]) > 1e-9) {
                sumPct += Math.abs(error / actual[i]);
            }
            sumActual += actual[i];
        }
        double mae = sumAbs / actual.length;
        double rmse = Math.sqrt(sumSq / actual.length);
        double mean = sumActual / actual.length;
        double sst = 0d;
        for (double v : actual) {
            double diff = v - mean;
            sst += diff * diff;
        }
        Double r2 = sst == 0 ? null : 1 - (sumSq / Math.max(sst, 1e-9));
        double mape = (sumPct / actual.length) * 100;
        return new ForecastEngineResponse.EvaluationMetrics(
            round(mae),
            round(rmse),
            round(mape),
            r2 != null ? round(r2) : null
        );
    }

    private List<Double> rollingWindowForecast(List<Double> historyValues, int periods) {
        List<Double> working = new ArrayList<>(historyValues);
        List<Double> results = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            int windowSize = Math.min(3, working.size());
            double sum = 0;
            for (int j = working.size() - windowSize; j < working.size(); j++) {
                sum += working.get(j);
            }
            double average = sum / windowSize;
            double variability = computeVariability(working);
            double forecast = average + variability * 0.2;
            results.add(forecast);
            working.add(forecast);
        }
        return results;
    }

    private List<Double> seasonalProjectionForecast(List<Double> historyValues, int periods) {
        int n = historyValues.size();
        double base = historyValues.get(n - 1);
        double trend = n > 1 ? (historyValues.get(n - 1) - historyValues.get(0)) / (n - 1) : 0;
        double seasonalAmplitude = computeVariability(historyValues) * 0.5;
        if (seasonalAmplitude == 0) {
            seasonalAmplitude = Math.abs(base) * 0.03;
        }

        List<Double> results = new ArrayList<>();
        for (int i = 1; i <= periods; i++) {
            double season = Math.sin((n + i) * Math.PI / 2);
            double forecast = base + trend * i + season * seasonalAmplitude;
            results.add(forecast);
        }
        return results;
    }

    private List<String> buildNextPeriods(List<ForecastEngineRequest.HistoryPoint> history,
                                          String frequency,
                                          int periods) {
        String lastPeriod = history.isEmpty() ? null : history.get(history.size() - 1).period();
        List<String> next = new ArrayList<>();
        if ("QUARTERLY".equalsIgnoreCase(frequency) && lastPeriod != null) {
            next.addAll(nextQuarters(lastPeriod, periods));
        } else {
            next.addAll(nextYears(lastPeriod, periods));
        }
        return next;
    }

    private List<String> nextYears(String lastPeriod, int periods) {
        int startYear = parseYear(lastPeriod).orElse(LocalDate.now().getYear());
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= periods; i++) {
            result.add(String.valueOf(startYear + i));
        }
        return result;
    }

    private Optional<Integer> parseYear(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(digits.substring(0, 4)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private List<String> nextQuarters(String lastPeriod, int periods) {
        Matcher matcher = QUARTER_PATTERN.matcher(lastPeriod.trim());
        int year;
        int quarter;
        if (matcher.matches()) {
            year = Integer.parseInt(matcher.group(1));
            quarter = Integer.parseInt(matcher.group(2));
        } else {
            year = parseYear(lastPeriod).orElse(LocalDate.now().getYear());
            quarter = 4;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            quarter++;
            if (quarter > 4) {
                quarter = 1;
                year++;
            }
            result.add(String.format(Locale.ROOT, "%d-Q%d", year, quarter));
        }
        return result;
    }

    private double computeConfidenceBand(List<Double> historyValues) {
        double stdDev = computeVariability(historyValues);
        if (stdDev == 0) {
            double last = historyValues.get(historyValues.size() - 1);
            return Math.max(Math.abs(last) * 0.04, 0.1);
        }
        return stdDev * 0.65;
    }

    private double computeVariability(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = 0;
        for (Double value : values) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= (values.size() - 1);
        return Math.sqrt(variance);
    }

    private ForecastEngineResponse.EvaluationMetrics buildMetrics(List<Double> historyValues,
                                                                  DoubleExponentialModel optimizedModel) {
        if (optimizedModel != null && optimizedModel.comparisons > 0) {
            return new ForecastEngineResponse.EvaluationMetrics(
                round(optimizedModel.mae),
                round(optimizedModel.rmse),
                round(optimizedModel.mape),
                optimizedModel.r2 != null ? round(optimizedModel.r2) : null
            );
        }
        return buildBaselineMetrics(historyValues);
    }

    private ForecastEngineResponse.EvaluationMetrics buildBaselineMetrics(List<Double> historyValues) {
        if (historyValues.size() < 2) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double sumAbs = 0;
        double sumSq = 0;
        double sumPct = 0;
        double sumActual = 0;
        int comparisons = 0;
        for (int i = 1; i < historyValues.size(); i++) {
            double actual = historyValues.get(i);
            double prev = historyValues.get(i - 1);
            double error = actual - prev;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (actual != 0) {
                sumPct += Math.abs(error / actual);
            }
            sumActual += actual;
            comparisons++;
        }
        if (comparisons == 0) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double mae = sumAbs / comparisons;
        double rmse = Math.sqrt(sumSq / comparisons);
        double mean = sumActual / comparisons;
        double sst = 0;
        for (int i = 1; i < historyValues.size(); i++) {
            double actual = historyValues.get(i);
            sst += Math.pow(actual - mean, 2);
        }
        Double r2 = sst == 0 ? null : 1 - (sumSq / Math.max(sst, 1e-9));
        double mape = (sumPct / comparisons) * 100;
        return new ForecastEngineResponse.EvaluationMetrics(
            round(mae),
            round(rmse),
            round(mape),
            r2 != null ? round(r2) : null
        );
    }

    private DoubleExponentialModel fitDoubleExponentialModel(List<Double> historyValues) {
        if (historyValues.size() < 3) {
            return null;
        }
        DoubleExponentialModel bestModel = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (double alpha = 0.2; alpha <= 0.95; alpha += 0.05) {
            for (double beta = 0.05; beta <= 0.5; beta += 0.05) {
                DoubleExponentialModel candidate = simulateDoubleExponential(historyValues, alpha, beta);
                if (candidate == null) {
                    continue;
                }
                if (candidate.mape < bestScore - 1e-6
                    || (Math.abs(candidate.mape - bestScore) < 1e-6
                        && (bestModel == null || candidate.rmse < bestModel.rmse))) {
                    bestScore = candidate.mape;
                    bestModel = candidate;
                }
            }
        }
        return bestModel;
    }

    private DoubleExponentialModel simulateDoubleExponential(List<Double> historyValues,
                                                              double alpha,
                                                              double beta) {
        double level = historyValues.get(0);
        double trend = historyValues.get(1) - historyValues.get(0);
        double sumAbs = 0;
        double sumSq = 0;
        double sumPct = 0;
        int comparisons = 0;
        for (int i = 1; i < historyValues.size(); i++) {
            double forecast = level + trend;
            double actual = historyValues.get(i);
            double error = actual - forecast;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            double denominator = Math.abs(actual) < 1e-6 ? 1e-6 : Math.abs(actual);
            sumPct += Math.abs(error) / denominator;
            comparisons++;
            double nextLevel = alpha * actual + (1 - alpha) * (level + trend);
            double nextTrend = beta * (nextLevel - level) + (1 - beta) * trend;
            level = nextLevel;
            trend = nextTrend;
        }
        if (comparisons == 0) {
            return null;
        }
        double mae = sumAbs / comparisons;
        double rmse = Math.sqrt(sumSq / comparisons);
        double mape = (sumPct / comparisons) * 100;
        double mean = historyValues.stream().skip(1).mapToDouble(Double::doubleValue).average().orElse(0d);
        double sst = 0d;
        for (int i = 1; i < historyValues.size(); i++) {
            double actual = historyValues.get(i);
            sst += Math.pow(actual - mean, 2);
        }
        Double r2 = sst == 0 ? null : 1 - (sumSq / Math.max(sst, 1e-9));
        return new DoubleExponentialModel(alpha, beta, level, trend, mae, rmse, mape, r2, comparisons);
    }

    private List<Double> projectDoubleExponential(DoubleExponentialModel model, int periods) {
        List<Double> results = new ArrayList<>();
        double baseLevel = model.level;
        double trend = model.trend;
        for (int i = 1; i <= periods; i++) {
            double forecast = baseLevel + trend * i;
            if (forecast < 0) {
                forecast = 0;
            }
            results.add(forecast);
        }
        return results;
    }

    private String generateRequestId() {
        return "local-" + System.currentTimeMillis();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
