package com.gxj.cropyield.modules.report.dto;

import java.util.List;

public record ReportDetailResponse(
    ReportSummaryResponse summary,
    List<ReportSectionResponse> sections
) {
}
