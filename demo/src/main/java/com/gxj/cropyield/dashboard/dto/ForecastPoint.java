package com.gxj.cropyield.dashboard.dto;

public record ForecastPoint(
        String label,
        double value,
        double lowerBound,
        double upperBound,
        String model
) {
}
