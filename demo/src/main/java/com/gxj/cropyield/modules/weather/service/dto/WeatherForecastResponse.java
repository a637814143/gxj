package com.gxj.cropyield.modules.weather.service.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 未来天气预报返回结构，封装 15 日逐日预报。
 */
public record WeatherForecastResponse(List<DailyForecast> daily) {

    public WeatherForecastResponse {
        daily = daily == null ? Collections.emptyList() : List.copyOf(daily);
    }

    public record DailyForecast(
        LocalDate date,
        Double maxTemperature,
        Double minTemperature,
        String conditionText,
        Double sunshineHours,
        String dataSource
    ) {
    }
}
