package com.gxj.demo.dashboard.dto;

import java.time.LocalDate;

public record UpcomingRenewalSummary(
        String softwareName,
        String categoryName,
        LocalDate maintenanceExpiryDate,
        long daysRemaining,
        String licenseType,
        String vendor,
        boolean hasRecentPrediction
) {
}
