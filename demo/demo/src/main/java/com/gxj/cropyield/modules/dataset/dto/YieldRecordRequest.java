package com.gxj.cropyield.modules.dataset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
/**
 * 数据集管理模块的数据传输对象（记录类型），在数据集管理场景下承载参数与返回值。
 */

public record YieldRecordRequest(
    @NotNull(message = "作物ID不能为空")
    Long cropId,

    @NotNull(message = "区域ID不能为空")
    Long regionId,

    @NotNull(message = "年份不能为空")
    @Min(value = 1900, message = "年份不能小于1900")
    Integer year,

    Double sownArea,

    Double production,

    Double yieldPerHectare,

    Double averagePrice,

    String dataSource,

    LocalDate collectedAt
) {
}
