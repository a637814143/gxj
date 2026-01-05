package com.gxj.cropyield.modules.forecast.dto;

import java.time.LocalDateTime;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastHistoryResponse(
    Long runId,
    Long forecastResultId,
    String period,
    Integer year,
    Long regionId,
    String regionName,
    Long cropId,
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
