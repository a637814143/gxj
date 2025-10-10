package com.gxj.cropyield.dashboard.dto;

public record RegionProductionSummary(
        String regionName,
        String level,
        double production,
        double sownArea,
        double yieldPerHectare
) {
}
