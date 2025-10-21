package com.gxj.cropyield.modules.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportGenerationRequest(
    @NotNull(message = "请选择分析区域")
    Long regionId,

    @NotNull(message = "请选择农作物")
    Long cropId,

    @Min(value = 1900, message = "开始年份不能早于1900年")
    @Max(value = 2100, message = "开始年份范围异常")
    Integer startYear,

    @Min(value = 1900, message = "结束年份不能早于1900年")
    @Max(value = 2100, message = "结束年份范围异常")
    Integer endYear,

    Boolean includePriceAnalysis,

    Boolean includeForecastComparison,

    Long forecastResultId,

    @Size(max = 128, message = "标题长度不能超过128位")
    String title,

    @Size(max = 512, message = "描述长度不能超过512位")
    String description,

    @Size(max = 128, message = "作者名称不能超过128位")
    String author
) {
}
