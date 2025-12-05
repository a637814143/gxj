package com.gxj.cropyield.modules.forecast.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleForecastModelRequest(@NotNull(message = "启用状态不能为空") Boolean enabled) {
}
