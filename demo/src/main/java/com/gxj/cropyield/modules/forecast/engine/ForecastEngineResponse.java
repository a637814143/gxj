package com.gxj.cropyield.modules.forecast.engine;

import java.util.List;

public record ForecastEngineResponse(
    String requestId,
    List<ForecastPoint> forecast,
    EvaluationMetrics metrics
) {

    public record ForecastPoint(String period, Double value, Double lowerBound, Double upperBound) {
    }

    public record EvaluationMetrics(Double mae, Double rmse, Double mape, Double r2) {
    }
}
