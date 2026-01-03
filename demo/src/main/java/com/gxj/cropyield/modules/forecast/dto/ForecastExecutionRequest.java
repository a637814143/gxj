package com.gxj.cropyield.modules.forecast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastExecutionRequest(
    @NotNull Long regionId,
    @NotNull Long cropId,
    @NotNull Long modelId,
    @Min(1) @Max(3) Integer forecastPeriods,
    @Min(1) Integer historyYears,
    String frequency,
    Map<String, Object> parameters
) {
}
