package com.gxj.cropyield.modules.system.dto;

import java.time.LocalDateTime;
/**
 * 系统设置模块的数据传输对象（记录类型），在系统设置场景下承载参数与返回值。
 */

public record SystemSettingResponse(
    Long id,
    Long defaultRegionId,
    String defaultRegionName,
    String notifyEmail,
    boolean clusterEnabled,
    int pendingChangeCount,
    String securityStrategy,
    boolean publicVisible,
    String publicTitle,
    String publicSummary,
    String publicAudience,
    String publicLevel,
    java.time.LocalDateTime publicStartAt,
    java.time.LocalDateTime publicEndAt,
    LocalDateTime updatedAt
) {
}
