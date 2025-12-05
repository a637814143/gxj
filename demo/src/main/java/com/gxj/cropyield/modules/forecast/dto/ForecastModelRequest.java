package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel.Granularity;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel.ModelType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    String description,

    @NotBlank(message = "适用作物不能为空")
    @Size(max = 128, message = "适用作物描述过长")
    String cropScope,

    @NotBlank(message = "适用区域不能为空")
    @Size(max = 128, message = "适用区域描述过长")
    String regionScope,

    @NotNull(message = "预测粒度不能为空")
    Granularity granularity,

    @NotNull(message = "历史窗口不能为空")
    @Positive(message = "历史窗口必须为正数")
    @Max(value = 120, message = "历史窗口不能超过120期")
    Integer historyWindow,

    @NotNull(message = "预测步数不能为空")
    @Positive(message = "预测步数必须为正数")
    @Max(value = 12, message = "预测步数不能超过12期")
    Integer forecastHorizon,

    @Size(max = 512, message = "超参数长度不能超过512位")
    String hyperParameters,

    @NotNull(message = "启用状态不能为空")
    Boolean enabled
) {
}
