package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastModelRequest(
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128位")
    String name,

    @NotNull(message = "模型类型不能为空")
    ModelType type,

    @Size(max = 512, message = "描述长度不能超过512位")
    String description
) {
}
