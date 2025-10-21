package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDateTime;

public record LoginLogSummaryResponse(
    long totalCount,
    long successCount,
    long failureCount,
    LocalDateTime lastSuccessTime,
    String lastSuccessUsername,
    LocalDateTime lastFailureTime,
    String lastFailureUsername
) {
}
