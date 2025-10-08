package com.dali.cropyield.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CropRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 64) String category,
        @Size(max = 64) String unit,
        @Size(max = 256) String description) {
}
