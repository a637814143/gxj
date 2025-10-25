package com.gxj.cropyield.modules.weather.service.dto;

import java.time.Instant;

/**
 * 天气服务返回的实时天气数据载体。
 */
public record WeatherRealtimeResponse(
    double longitude,
    double latitude,
    String status,
    String skyCondition,
    double temperature,
    double apparentTemperature,
    double humidity,
    double pressure,
    double visibility,
    Wind wind,
    Precipitation precipitation,
    AirQuality airQuality,
    String forecastKeypoint,
    String precipitationDescription,
    Instant observationTime,
    Instant fetchedAt
) {

    public record Wind(double speed, double direction) {
    }

    public record Precipitation(double localIntensity, Double nearestIntensity, Double nearestDistance) {
    }

    public record AirQuality(
        Double aqi,
        Double pm25,
        Double pm10,
        Double o3,
        Double so2,
        Double no2,
        Double co,
        String description,
        String category
    ) {
    }
}
