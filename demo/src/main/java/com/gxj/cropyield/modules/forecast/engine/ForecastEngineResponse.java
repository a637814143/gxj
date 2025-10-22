package com.gxj.cropyield.modules.forecast.engine;

import java.util.List;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

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
