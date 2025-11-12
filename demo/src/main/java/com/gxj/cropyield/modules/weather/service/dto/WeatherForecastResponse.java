package com.gxj.cropyield.modules.weather.service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 逐日天气预报响应数据结构。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeatherForecastResponse(Instant fetchedAt, List<DailyForecast> daily) {

    public WeatherForecastResponse {
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
        daily = daily == null ? List.of() : List.copyOf(daily);
    }

    /**
     * 单日天气预报数据。
     */
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
