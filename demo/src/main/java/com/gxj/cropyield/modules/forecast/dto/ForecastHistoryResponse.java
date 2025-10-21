package com.gxj.cropyield.modules.forecast.dto;

import java.time.LocalDateTime;

public record ForecastHistoryResponse(
    Long runId,
    Long forecastResultId,
    String period,
    Integer year,
    String regionName,
    String cropName,
    String modelName,
    String modelType,
    String measurementLabel,
    String measurementUnit,
    Double measurementValue,
    Double predictedProduction,
    Double predictedYield,
    Double sownArea,
    Double averagePrice,
    Double estimatedRevenue,
    LocalDateTime generatedAt
) {
}
