package com.gxj.demo.dashboard.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long softwareCount,
        long predictedSoftwareCount,
        long predictionCount,
        long categoryCount,
        List<TrendPoint> recentPredictionTrend,
        List<RecentPredictionSummary> latestPredictions,
        List<UpcomingRenewalSummary> upcomingRenewals
) {
}
