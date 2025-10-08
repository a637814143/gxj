package com.dali.cropyield.modules.forecast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForecastModelRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 64) String algorithm,
        @Size(max = 256) String description,
        String hyperParameters) {
}
