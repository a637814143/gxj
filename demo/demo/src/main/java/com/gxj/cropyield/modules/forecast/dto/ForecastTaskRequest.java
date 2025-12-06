package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastTask.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastTaskRequest(
    @NotNull(message = "模型ID不能为空")
    Long modelId,

    @NotNull(message = "作物ID不能为空")
    Long cropId,

    @NotNull(message = "区域ID不能为空")
    Long regionId,

    TaskStatus status,

    @Size(max = 512, message = "参数长度不能超过512位")
    String parameters
) {
}
