package com.gxj.cropyield.modules.report.dto;

import com.fasterxml.jackson.databind.JsonNode;
/**
 * 报表分析模块的数据传输对象（记录类型），在报表分析场景下承载参数与返回值。
 */

public record ReportSectionResponse(
    Long id,
    String type,
    String title,
    String description,
    JsonNode data,
    Integer sortOrder
) {
}
