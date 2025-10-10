package com.gxj.cropyield.dashboard.dto;

import java.util.List;

public record DashboardSummaryResponse(
        double totalProduction,
        double totalSownArea,
        double averageYield,
        long recordCount,
        List<TrendPoint> productionTrend,
        List<CropShare> cropStructure,
        List<RegionProductionSummary> regionComparisons,
        List<RecentYieldRecord> recentRecords,
        List<ForecastPoint> forecastOutlook
) {
}
