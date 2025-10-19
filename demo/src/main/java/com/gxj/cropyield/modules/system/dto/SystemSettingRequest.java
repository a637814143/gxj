package com.gxj.cropyield.modules.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SystemSettingRequest(
    Long defaultRegionId,
    @Size(max = 128, message = "通知邮箱长度不能超过128位")
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    String notifyEmail,
    Boolean clusterEnabled,
    @Min(value = 0, message = "待审批变更数量不能小于0")
    Integer pendingChangeCount,
    @Size(max = 64, message = "安全策略描述不能超过64位")
    String securityStrategy
) {
}
