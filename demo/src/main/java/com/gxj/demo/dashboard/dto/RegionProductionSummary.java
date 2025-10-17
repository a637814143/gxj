package com.gxj.demo.dashboard.dto;

public record RegionProductionSummary(
        String regionName,
        String level,
        double production,
        double sownArea,
        double yieldPerHectare
) {
}
