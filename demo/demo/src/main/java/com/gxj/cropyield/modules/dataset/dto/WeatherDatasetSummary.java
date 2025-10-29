package com.gxj.cropyield.modules.dataset.dto;

import java.time.LocalDate;

/**
 * 气象数据聚合指标。
 */
public record WeatherDatasetSummary(
        Long recordCount,
        Double averageMaxTemperature,
        Double averageMinTemperature,
        Double maxTemperature,
        LocalDate maxTemperatureDate,
        Double minTemperature,
        LocalDate minTemperatureDate,
        Double totalSunshineHours,
        Double averageSunshineHours,
        String dominantWeather,
        Double dominantWeatherRatio
) {
}
