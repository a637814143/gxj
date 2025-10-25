package com.gxj.cropyield.modules.report.dto;

import java.util.List;
/**
 * 报表分析模块的数据传输对象（记录类型），在报表分析场景下承载参数与返回值。
 */

public record ReportDetailResponse(
    ReportSummaryResponse summary,
    List<ReportSectionResponse> sections
) {
}
