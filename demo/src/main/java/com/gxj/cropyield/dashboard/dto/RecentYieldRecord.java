package com.gxj.cropyield.dashboard.dto;

import java.time.LocalDate;

public record RecentYieldRecord(
        Long runId,
        String cropName,
        String regionName,
        int year,
        double sownArea,
        double production,
        double yieldPerHectare,
        Double averagePrice,
        double estimatedRevenue,
        LocalDate collectedAt
) {
}
