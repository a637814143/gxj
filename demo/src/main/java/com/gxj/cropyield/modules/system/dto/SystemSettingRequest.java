package com.gxj.cropyield.modules.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
/**
 * 系统设置模块的数据传输对象（记录类型），在系统设置场景下承载参数与返回值。
 */

public record SystemSettingRequest(
    Long defaultRegionId,
    @Size(max = 128, message = "通知邮箱长度不能超过128位")
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    String notifyEmail,
    Boolean clusterEnabled,
    @Min(value = 0, message = "待审批变更数量不能小于0")
    Integer pendingChangeCount,
    @Size(max = 64, message = "安全策略描述不能超过64位")
    String securityStrategy,
    @Size(max = 128, message = "通知标题长度不能超过128位")
    String announcementTitle,
    @Size(max = 512, message = "通知内容长度不能超过512位")
    String announcementMessage,
    @Size(max = 32, message = "通知状态长度不能超过32位")
    String announcementStatus
) {
}
