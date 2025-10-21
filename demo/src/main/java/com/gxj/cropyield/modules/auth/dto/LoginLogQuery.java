package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDate;

public record LoginLogQuery(
    String username,
    Boolean success,
    LocalDate startDate,
    LocalDate endDate
) {
}
