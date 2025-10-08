package com.dali.cropyield.modules.dataset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceRecordRequest(
        @NotNull Long cropId,
        @NotNull @Min(1900) Integer year,
        BigDecimal averagePrice,
        String unit,
        String dataSource) {
}
