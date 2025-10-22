package com.gxj.cropyield.dashboard.dto;

import java.util.List;
/**
 * 驾驶舱统计模块的数据传输对象（记录类型），在驾驶舱统计场景下承载参数与返回值。
 */

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
