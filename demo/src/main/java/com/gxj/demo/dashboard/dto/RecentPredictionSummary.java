package com.gxj.demo.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentPredictionSummary(
        String softwareName,
        String categoryName,
        LocalDate predictionDate,
        BigDecimal predictedAnnualCost,
        Integer predictedActiveUsers,
        Double confidence
) {
}
