package com.example.demo.yielddata;

import java.time.LocalDate;

public record YieldRecordResponse(
        Long id,
        String cropName,
        String cropCategory,
        String regionName,
        String regionLevel,
        Integer year,
        Double sownArea,
        Double production,
        Double yieldPerHectare,
        Double averagePrice,
        Double estimatedRevenue,
        String dataSource,
        LocalDate collectedAt
) {
}
