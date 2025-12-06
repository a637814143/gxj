package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDateTime;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

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
