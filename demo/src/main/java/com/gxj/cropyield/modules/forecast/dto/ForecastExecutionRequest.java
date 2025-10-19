package com.gxj.cropyield.modules.forecast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ForecastExecutionRequest(
    @NotNull Long regionId,
    @NotNull Long cropId,
    @NotNull Long modelId,
    @Min(1) @Max(3) Integer forecastPeriods,
    @Min(1) Integer historyYears,
    String frequency
) {
}
