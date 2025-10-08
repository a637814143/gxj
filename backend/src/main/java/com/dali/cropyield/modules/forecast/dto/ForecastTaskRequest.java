package com.dali.cropyield.modules.forecast.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ForecastTaskRequest(
        @NotNull Long cropId,
        @NotNull Long regionId,
        Long modelId,
        @NotNull @Min(1900) Integer startYear,
        @NotNull @Min(1900) Integer endYear,
        @NotNull @Min(1) Integer horizonYears) {
}
