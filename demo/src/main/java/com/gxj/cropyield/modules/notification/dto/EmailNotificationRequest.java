package com.gxj.cropyield.modules.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 邮件通知请求模型，支持按模板渲染邮件内容。
 */
public record EmailNotificationRequest(
    @NotBlank @Email String to,
    @NotBlank String subject,
    @NotBlank String template,
    Map<String, String> context
) {
}
