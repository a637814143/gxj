package com.gxj.cropyield.modules.forecast.dto;

import java.time.LocalDateTime;
import java.util.List;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastExecutionResponse(
    Long runId,
    Metadata metadata,
    List<SeriesPoint> history,
    List<SeriesPoint> forecast,
    EvaluationMetrics metrics,
    Long forecastResultId
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
