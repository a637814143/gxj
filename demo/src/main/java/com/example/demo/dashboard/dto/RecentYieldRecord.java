package com.example.demo.dashboard.dto;

import java.time.LocalDate;

public record RecentYieldRecord(
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
