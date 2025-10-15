package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.config.ForecastEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ForecastEngineClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastEngineClient.class);

    private final RestTemplate restTemplate;
    private final ForecastEngineProperties properties;

    public ForecastEngineClient(RestTemplate forecastRestTemplate, ForecastEngineProperties properties) {
        this.restTemplate = forecastRestTemplate;
        this.properties = properties;
    }

    public ForecastEngineResponse runForecast(ForecastEngineRequest request) {
        if (StringUtils.hasText(properties.getBaseUrl())) {
            try {
                ResponseEntity<ForecastEngineResponse> response = restTemplate.postForEntity(
                    properties.getBaseUrl(), request, ForecastEngineResponse.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
                log.warn("模型服务返回非成功状态: {}", response.getStatusCode());
            } catch (RestClientException ex) {
                log.warn("调用外部模型服务失败，将使用内置回退逻辑: {}", ex.getMessage());
            }
        }
        return fallbackForecast(request);
    }

    private ForecastEngineResponse fallbackForecast(ForecastEngineRequest request) {
        List<ForecastEngineResponse.ForecastPoint> forecastPoints = new ArrayList<>();

        double baseValue = request.history().isEmpty()
            ? 0d
            : request.history().get(request.history().size() - 1).value();
        double growthRate = estimateGrowthRate(request.history());

        int lastYear = extractLastYear(request.history());
        for (int i = 1; i <= request.forecastPeriods(); i++) {
            double predicted = baseValue * Math.pow(1 + growthRate, i);
            double deviation = predicted * 0.05;
            String period = lastYear > 0 ? String.valueOf(lastYear + i) : String.valueOf(i);
            forecastPoints.add(new ForecastEngineResponse.ForecastPoint(
                period,
                round(predicted),
                round(predicted - deviation),
                round(predicted + deviation)
            ));
        }

        ForecastEngineResponse.EvaluationMetrics metrics = buildMetrics(request.history());
        return new ForecastEngineResponse(generateRequestId(), forecastPoints, metrics);
    }

    private double estimateGrowthRate(List<ForecastEngineRequest.HistoryPoint> history) {
        if (history.size() < 2) {
            return 0.02;
        }
        double first = history.get(0).value() != null ? history.get(0).value() : 0d;
        double last = history.get(history.size() - 1).value() != null ? history.get(history.size() - 1).value() : first;
        if (first == 0) {
            return 0.02;
        }
        return Math.max(-0.1, Math.min((last - first) / Math.abs(first) / Math.max(history.size() - 1, 1), 0.12));
    }

    private ForecastEngineResponse.EvaluationMetrics buildMetrics(List<ForecastEngineRequest.HistoryPoint> history) {
        if (history.size() < 2) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double sumAbs = 0;
        double sumSq = 0;
        double sumPct = 0;
        double sumActual = 0;
        double sumActualSq = 0;
        int comparisons = 0;
        for (int i = 1; i < history.size(); i++) {
            Double actual = history.get(i).value();
            Double prev = history.get(i - 1).value();
            if (actual == null || prev == null) {
                continue;
            }
            double error = actual - prev;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (actual != 0) {
                sumPct += Math.abs(error / actual);
            }
            sumActual += actual;
            sumActualSq += actual * actual;
            comparisons++;
        }
        if (comparisons == 0) {
            return new ForecastEngineResponse.EvaluationMetrics(null, null, null, null);
        }
        double mae = sumAbs / comparisons;
        double rmse = Math.sqrt(sumSq / comparisons);
        double mean = sumActual / comparisons;
        double sst = 0;
        for (int i = 1; i < history.size(); i++) {
            Double actual = history.get(i).value();
            if (actual != null) {
                sst += Math.pow(actual - mean, 2);
            }
        }
        Double r2 = sst == 0 ? null : 1 - (sumSq / Math.max(sst, 1e-9));
        double mape = (sumPct / comparisons) * 100;
        return new ForecastEngineResponse.EvaluationMetrics(round(mae), round(rmse), round(mape), r2 != null ? round(r2) : null);
    }

    private int extractLastYear(List<ForecastEngineRequest.HistoryPoint> history) {
        if (history.isEmpty()) {
            return LocalDate.now().getYear();
        }
        try {
            return Integer.parseInt(history.get(history.size() - 1).period());
        } catch (NumberFormatException ex) {
            return LocalDate.now().getYear();
        }
    }

    private String generateRequestId() {
        return "local-" + System.currentTimeMillis();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
