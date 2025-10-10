package com.gxj.cropyield.modules.dataset.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PriceRecordRequest(
    @NotNull(message = "作物ID不能为空")
    Long cropId,

    @NotNull(message = "区域ID不能为空")
    Long regionId,

    @NotNull(message = "记录日期不能为空")
    LocalDate recordDate,

    @NotNull(message = "价格不能为空")
    Double price
) {
}
