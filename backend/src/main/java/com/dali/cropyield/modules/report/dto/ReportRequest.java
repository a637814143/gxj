package com.dali.cropyield.modules.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 512) String summary,
        @Size(max = 256) String fileUrl,
        @Size(max = 64) String generatedBy) {
}
