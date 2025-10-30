package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private interface ForecastComputation {
        List<Double> forecast(List<Double> historyValues, int periods);
    }

    private final Map<ForecastModel.ModelType, ForecastComputation> computations = new EnumMap<>(ForecastModel.ModelType.class);
    private final Dl4jLstmForecaster lstmForecaster = new Dl4jLstmForecaster();
    private final SmileHoltWintersForecaster holtWintersForecaster = new SmileHoltWintersForecaster();

    public LocalForecastEngine() {
        computations.put(ForecastModel.ModelType.ARIMA, this::linearTrendForecast);
        computations.put(ForecastModel.ModelType.LSTM, (historyValues, periods) ->
            lstmForecaster.forecast(historyValues, periods)
                .orElseGet(() -> exponentialSmoothingForecast(historyValues, periods))
        );
        computations.put(ForecastModel.ModelType.RANDOM_FOREST, this::rollingWindowForecast);
        computations.put(ForecastModel.ModelType.PROPHET, (historyValues, periods) ->
            holtWintersForecaster.forecast(historyValues, periods)
                .orElseGet(() -> seasonalProjectionForecast(historyValues, periods))
        );
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

        if (modelType == ForecastModel.ModelType.WEATHER_REGRESSION) {
            return forecastWithWeather(sanitizedHistory, forecastPeriods, request.frequency());
        }

        List<Double> historyValues = sanitizedHistory.stream()
            .map(ForecastEngineRequest.HistoryPoint::value)
            .toList();

        DoubleExponentialModel optimizedModel = null;
        List<Double> rawForecast;
        if (modelType == ForecastModel.ModelType.ARIMA) {
            optimizedModel = fitDoubleExponentialModel(historyValues);
            if (optimizedModel != null) {
                rawForecast = projectDoubleExponential(optimizedModel, forecastPeriods);
            } else {
                rawForecast = linearTrendForecast(historyValues, forecastPeriods);
            }
        } else {
            ForecastComputation computation = computations.getOrDefault(modelType, this::linearTrendForecast);
            rawForecast = computation.forecast(historyValues, forecastPeriods);
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

        ForecastEngineResponse.EvaluationMetrics metrics = buildMetrics(historyValues, optimizedModel);
        return new ForecastEngineResponse(generateRequestId(), points, metrics);
    }

    private ForecastEngineResponse forecastWithWeather(List<ForecastEngineRequest.HistoryPoint> history,
                                                       int forecastPeriods,
                                                       String frequency) {
        List<Double> historyValues = history.stream()
            .map(ForecastEngineRequest.HistoryPoint::value)
            .filter(Objects::nonNull)
            .toList();
        if (historyValues.isEmpty()) {
            return new ForecastEngineResponse(
                generateRequestId(),
                List.of(),
                new ForecastEngineResponse.EvaluationMetrics(null, null, null, null)
            );
        }

        List<ForecastEngineRequest.HistoryPoint> training = new ArrayList<>();
        for (ForecastEngineRequest.HistoryPoint point : history) {
            if (point.value() != null && point.features() != null && !point.features().isEmpty()) {
                training.add(point);
            }
        }
        if (training.size() < 2) {
            return buildFallbackResponse(history, historyValues, forecastPeriods, frequency);
        }

        LinkedHashSet<String> featureNames = new LinkedHashSet<>();
        for (ForecastEngineRequest.HistoryPoint point : training) {
            for (Map.Entry<String, Double> entry : point.features().entrySet()) {
                if (entry.getValue() != null) {
                    featureNames.add(entry.getKey());
                }
            }
        }
        if (featureNames.isEmpty()) {
            return buildFallbackResponse(history, historyValues, forecastPeriods, frequency);
        }

        List<double[]> rows = new ArrayList<>();
        List<Double> targets = new ArrayList<>();
        List<Integer> years = new ArrayList<>();
        List<Map<String, Double>> orderedFeatures = new ArrayList<>();

        for (ForecastEngineRequest.HistoryPoint point : training) {
            Map<String, Double> features = point.features();
            double[] row = new double[featureNames.size() + 1];
            row[0] = 1d;
            boolean missing = false;
            int idx = 1;
            for (String name : featureNames) {
                Double value = features.get(name);
                if (value == null) {
                    missing = true;
                    break;
                }
                row[idx++] = value;
            }
            if (missing) {
                continue;
            }
            Optional<Integer> yearOpt = parseYear(point.period());
            if (yearOpt.isEmpty()) {
                continue;
            }
            rows.add(row);
            targets.add(point.value());
            years.add(yearOpt.get());
            Map<String, Double> sanitized = new LinkedHashMap<>();
            for (String name : featureNames) {
                sanitized.put(name, features.get(name));
            }
            orderedFeatures.add(sanitized);
        }

        if (rows.size() < 2) {
            return buildFallbackResponse(history, historyValues, forecastPeriods, frequency);
        }

        double[] coefficients = solveNormalEquations(rows, targets);
        if (coefficients == null) {
            return buildFallbackResponse(history, historyValues, forecastPeriods, frequency);
        }

        List<Double> trainingPredictions = new ArrayList<>();
        for (double[] row : rows) {
            trainingPredictions.add(dot(row, coefficients));
        }

        Map<String, List<Double>> featureSeries = new LinkedHashMap<>();
        for (String name : featureNames) {
            List<Double> values = new ArrayList<>();
            for (Map<String, Double> sanitized : orderedFeatures) {
                values.add(sanitized.get(name));
            }
            featureSeries.put(name, values);
        }

        Map<String, double[]> projected = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : featureSeries.entrySet()) {
            projected.put(entry.getKey(), projectFeatureTrend(years, entry.getValue(), forecastPeriods));
        }

        List<Double> rawForecast = new ArrayList<>();
        for (int i = 0; i < forecastPeriods; i++) {
            double[] featureRow = new double[coefficients.length];
            featureRow[0] = 1d;
            int idx = 1;
            for (String name : featureNames) {
                double[] projection = projected.get(name);
                double value = projection.length > i ? projection[i] : projection[projection.length - 1];
                featureRow[idx++] = value;
            }
            double prediction = dot(featureRow, coefficients);
            if (prediction < 0) {
                prediction = 0;
            }
            rawForecast.add(prediction);
        }

        List<String> nextPeriods = buildNextPeriods(history, frequency, rawForecast.size());
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

        ForecastEngineResponse.EvaluationMetrics metrics = buildRegressionMetrics(targets, trainingPredictions);
        return new ForecastEngineResponse(generateRequestId(), points, metrics);
    }

    private ForecastEngineResponse buildFallbackResponse(List<ForecastEngineRequest.HistoryPoint> history,
                                                         List<Double> historyValues,
                                                         int forecastPeriods,
                                                         String frequency) {
        if (historyValues.isEmpty()) {
            return new ForecastEngineResponse(
                generateRequestId(),
                List.of(),
                new ForecastEngineResponse.EvaluationMetrics(null, null, null, null)
            );
        }
        List<Double> fallback = linearTrendForecast(historyValues, forecastPeriods);
        List<String> nextPeriods = buildNextPeriods(history, frequency, fallback.size());
        double confidenceBand = computeConfidenceBand(historyValues);
        List<ForecastEngineResponse.ForecastPoint> points = new ArrayList<>();
        for (int i = 0; i < fallback.size(); i++) {
            double value = fallback.get(i);
            double lower = Math.max(0d, value - confidenceBand);
            double upper = value + confidenceBand;
            points.add(new ForecastEngineResponse.ForecastPoint(
                nextPeriods.get(i),
                round(value),
                round(lower),
                round(upper)
            ));
        }
        ForecastEngineResponse.EvaluationMetrics metrics = buildBaselineMetrics(historyValues);
        return new ForecastEngineResponse(generateRequestId(), points, metrics);
    }

    private ForecastModel.ModelType resolveModelType(String code) {
        if (code == null) {
            return ForecastModel.ModelType.ARIMA;
        }
        try {
            return ForecastModel.ModelType.valueOf(code.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ForecastModel.ModelType.ARIMA;
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

    private double[] solveNormalEquations(List<double[]> rows, List<Double> targets) {
        if (rows.isEmpty()) {
            return null;
        }
        int dimension = rows.get(0).length;
        double[][] xtx = new double[dimension][dimension];
        double[] xty = new double[dimension];
        for (int i = 0; i < rows.size(); i++) {
            double[] row = rows.get(i);
            double y = targets.get(i);
            for (int j = 0; j < dimension; j++) {
                xty[j] += row[j] * y;
                for (int k = 0; k < dimension; k++) {
                    xtx[j][k] += row[j] * row[k];
                }
            }
        }
        return solveLinearSystem(xtx, xty);
    }

    private double[] solveLinearSystem(double[][] matrix, double[] vector) {
        int n = vector.length;
        double[][] augmented = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n] = vector[i];
        }
        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int r = i + 1; r < n; r++) {
                if (Math.abs(augmented[r][i]) > Math.abs(augmented[pivot][i])) {
                    pivot = r;
                }
            }
            if (Math.abs(augmented[pivot][i]) < 1e-9) {
                return null;
            }
            if (pivot != i) {
                double[] temp = augmented[i];
                augmented[i] = augmented[pivot];
                augmented[pivot] = temp;
            }
            double pivotValue = augmented[i][i];
            for (int j = i; j <= n; j++) {
                augmented[i][j] /= pivotValue;
            }
            for (int r = 0; r < n; r++) {
                if (r == i) {
                    continue;
                }
                double factor = augmented[r][i];
                for (int j = i; j <= n; j++) {
                    augmented[r][j] -= factor * augmented[i][j];
                }
            }
        }
        double[] solution = new double[n];
        for (int i = 0; i < n; i++) {
            solution[i] = augmented[i][n];
        }
        return solution;
    }

    private double[] projectFeatureTrend(List<Integer> years, List<Double> values, int periods) {
        double[] projection = new double[Math.max(periods, 0)];
        if (projection.length == 0) {
            return projection;
        }
        if (years.isEmpty() || values.isEmpty()) {
            Arrays.fill(projection, 0d);
            return projection;
        }
        if (years.size() < 2) {
            double last = values.get(values.size() - 1);
            Arrays.fill(projection, last);
            return projection;
        }
        double n = years.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < years.size(); i++) {
            double x = years.get(i);
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        int lastYear = years.get(years.size() - 1);
        for (int i = 0; i < projection.length; i++) {
            int futureYear = lastYear + i + 1;
            projection[i] = intercept + slope * futureYear;
        }
        return projection;
    }

    private ForecastEngineResponse.EvaluationMetrics buildRegressionMetrics(List<Double> actual,
                                                                            List<Double> predicted) {
        if (actual.isEmpty() || actual.size() != predicted.size()) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double sumAbs = 0;
        double sumSq = 0;
        double sumPct = 0;
        int pctCount = 0;
        double mean = actual.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double ssTot = 0;
        for (Double value : actual) {
            ssTot += Math.pow(value - mean, 2);
        }
        for (int i = 0; i < actual.size(); i++) {
            double a = actual.get(i);
            double p = predicted.get(i);
            double error = a - p;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (a != 0) {
                sumPct += Math.abs(error / a);
                pctCount++;
            }
        }
        int n = actual.size();
        double mae = sumAbs / n;
        double rmse = Math.sqrt(sumSq / n);
        Double mape = pctCount > 0 ? (sumPct / pctCount) * 100 : null;
        Double r2 = ssTot == 0 ? null : 1 - (sumSq / ssTot);
        return new ForecastEngineResponse.EvaluationMetrics(
            round(mae),
            round(rmse),
            mape != null ? round(mape) : null,
            r2 != null ? round(r2) : null
        );
    }

    private double dot(double[] left, double[] right) {
        double result = 0;
        int limit = Math.min(left.length, right.length);
        for (int i = 0; i < limit; i++) {
            result += left[i] * right[i];
        }
        return result;
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
