package com.gxj.cropyield.modules.forecast.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ForecastExecutionResponse(
    Long runId,
    Metadata metadata,
    List<SeriesPoint> history,
    List<SeriesPoint> forecast,
    EvaluationMetrics metrics
) {

    public record Metadata(
        String regionName,
        String cropName,
        String modelName,
        String modelType,
        String frequency,
        Integer forecastPeriods,
        LocalDateTime generatedAt,
        String valueLabel,
        String valueUnit
    ) {
    }

    public record SeriesPoint(String period, Double value, Double lowerBound, Double upperBound) {
    }

    public record EvaluationMetrics(Double mae, Double rmse, Double mape, Double r2) {
    }
}
