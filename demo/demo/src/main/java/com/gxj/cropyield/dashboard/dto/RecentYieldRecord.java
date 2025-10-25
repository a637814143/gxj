package com.gxj.cropyield.dashboard.dto;

import java.time.LocalDate;
/**
 * 驾驶舱统计模块的数据传输对象（记录类型），在驾驶舱统计场景下承载参数与返回值。
 */

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
