package com.gxj.cropyield.modules.forecast.engine;

import java.util.List;
import java.util.Map;

public record ForecastEngineRequest(
    String modelCode,
    String frequency,
    int forecastPeriods,
    List<HistoryPoint> history,
    Map<String, Object> parameters
) {

    public record HistoryPoint(String period, Double value) {
    }
}
