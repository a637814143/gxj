package com.gxj.cropyield.modules.report.dto;

import java.util.List;

public record ReportOverviewResponse(
    List<ReportSummaryResponse> reports,
    ReportMetrics metrics
) {
}
