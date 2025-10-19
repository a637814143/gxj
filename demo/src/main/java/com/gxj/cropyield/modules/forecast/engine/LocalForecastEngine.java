package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalForecastEngine {

    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})[-_/]?Q([1-4])$");

    private interface ForecastComputation {
        List<Double> forecast(List<Double> historyValues, int periods);
    }

    private final Map<ForecastModel.ModelType, ForecastComputation> computations = new EnumMap<>(ForecastModel.ModelType.class);

    public LocalForecastEngine() {
        computations.put(ForecastModel.ModelType.ARIMA, this::linearTrendForecast);
        computations.put(ForecastModel.ModelType.LSTM, this::exponentialSmoothingForecast);
        computations.put(ForecastModel.ModelType.RANDOM_FOREST, this::rollingWindowForecast);
        computations.put(ForecastModel.ModelType.PROPHET, this::seasonalProjectionForecast);
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
        List<Double> historyValues = sanitizedHistory.stream()
            .map(ForecastEngineRequest.HistoryPoint::value)
            .toList();

        ForecastModel.ModelType modelType = resolveModelType(request.modelCode());
        ForecastComputation computation = computations.getOrDefault(modelType, this::linearTrendForecast);
        int forecastPeriods = Math.max(1, Math.min(request.forecastPeriods(), 3));

        List<Double> rawForecast = computation.forecast(historyValues, forecastPeriods);
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

        ForecastEngineResponse.EvaluationMetrics metrics = buildMetrics(historyValues);
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
            sanitized.add(new ForecastEngineRequest.HistoryPoint(point.period(), value));
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

    private ForecastEngineResponse.EvaluationMetrics buildMetrics(List<Double> historyValues) {
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

    private String generateRequestId() {
        return "local-" + System.currentTimeMillis();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
