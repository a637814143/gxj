package com.gxj.cropyield.modules.forecast.engine;

import java.util.List;
import java.util.Map;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastEngineRequest(
    String modelCode,
    String frequency,
    int forecastPeriods,
    List<HistoryPoint> history,
    Map<String, Object> parameters
) {

    public record HistoryPoint(String period, Double value, Map<String, Double> features) {
    }
}
