package com.gxj.cropyield.modules.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
    @NotBlank(message = "报告标题不能为空")
    @Size(max = 128, message = "标题长度不能超过128位")
    String title,

    @Size(max = 512, message = "描述长度不能超过512位")
    String description,

    @NotNull(message = "预测结果ID不能为空")
    Long forecastResultId,

    @Size(max = 2048, message = "洞察长度不能超过2048字符")
    String insights
) {
}
