package com.gxj.cropyield.modules.system.dto;

import java.time.LocalDateTime;

public record SystemSettingResponse(
    Long id,
    Long defaultRegionId,
    String defaultRegionName,
    String notifyEmail,
    boolean clusterEnabled,
    int pendingChangeCount,
    String securityStrategy,
    LocalDateTime updatedAt
) {
}
