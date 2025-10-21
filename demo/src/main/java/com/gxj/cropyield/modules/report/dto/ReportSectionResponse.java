package com.gxj.cropyield.modules.report.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ReportSectionResponse(
    Long id,
    String type,
    String title,
    String description,
    JsonNode data,
    Integer sortOrder
) {
}
