package com.gxj.cropyield.modules.report.dto;

public record ReportMetrics(
    long totalReports,
    long publishedThisMonth,
    long pendingApproval,
    boolean autoGenerationEnabled
) {
}
