package com.example.demo.dashboard.dto;

public record ForecastPoint(
        String label,
        double value,
        double lowerBound,
        double upperBound,
        String model
) {
}
