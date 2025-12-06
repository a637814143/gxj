package com.gxj.cropyield.modules.notification.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.mail.EmailSender;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.notification.dto.EmailNotificationRequest;
import com.gxj.cropyield.modules.notification.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);

    private final EmailSender emailSender;

    public EmailNotificationServiceImpl(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendEmailNotification(EmailNotificationRequest request) {
        String to = StringUtils.trimWhitespace(request.to());
        String subject = Optional.ofNullable(request.subject())
            .map(StringUtils::trimWhitespace)
            .filter(StringUtils::hasText)
            .orElse("系统通知");
        String template = Optional.ofNullable(request.template())
            .map(String::trim)
            .map(String::toUpperCase)
            .orElse("GENERAL");
        Map<String, String> context = Objects.requireNonNullElse(request.context(), Map.of());

        String html = renderTemplate(template, subject, context);
        try {
            emailSender.sendHtmlAsync(to, subject, html).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("Failed to send notification email to {}: {}", to, ex.getMessage(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "邮件发送失败，请检查邮箱服务配置");
        }
    }

    private String renderTemplate(String template, String subject, Map<String, String> context) {
        return switch (template) {
            case "REGISTRATION_SUCCESS" -> buildRegistrationSuccess(context);
            case "PASSWORD_RESET" -> buildPasswordReset(context);
            default -> buildGeneralTemplate(subject, context);
        };
    }

    private String buildRegistrationSuccess(Map<String, String> context) {
        String username = StringUtils.trimWhitespace(context.getOrDefault("username", "用户"));
        String brandTag = "<div style=\\\"display:inline-block;padding:6px 12px;margin-bottom:8px;border-radius:10px;background:#0f172a;color:#fff;font-weight:700;letter-spacing:0.5px\\\">农作物产量平台</div>";
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.6\">" +
            brandTag +
            "<h2>注册成功通知</h2>" +
            "<p>您好，" + username + "：</p>" +
            "<p>您的账号已成功注册并激活，现在可以登录系统。</p>" +
            "<p>如果并非本人操作，请尽快联系管理员。</p>" +
            "<p style=\"color:#888\">发送时间：" + LocalDateTime.now() + "</p>" +
            "</div>";
    }

    private String buildPasswordReset(Map<String, String> context) {
        String username = StringUtils.trimWhitespace(context.getOrDefault("username", "用户"));
        String newPassword = StringUtils.trimWhitespace(context.getOrDefault("newPassword", "(已重置密码)"));
        String brandTag = "<div style=\"display:inline-block;padding:6px 12px;margin-bottom:8px;border-radius:10px;background:#0f172a;color:#fff;font-weight:700;letter-spacing:0.5px\">农作物产量平台</div>";
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.6\">" +
            brandTag +
            "<h2>密码重置通知</h2>" +
            "<p>您好，" + username + "：</p>" +
            "<p>管理员已为您的账户重置密码，新密码为：<strong style=\"font-size:18px\">" + newPassword + "</strong></p>" +
            "<p>请尽快登录并修改为您熟悉的密码，避免泄露。</p>" +
            "<p style=\"color:#888\">发送时间：" + LocalDateTime.now() + "</p>" +
            "</div>";
    }

    private String buildGeneralTemplate(String subject, Map<String, String> context) {
        StringBuilder builder = new StringBuilder("<div style=\"font-family:Arial,sans-serif;line-height:1.6\">");
        builder.append("<h2>").append(subject).append("</h2>");
        if (!CollectionUtils.isEmpty(context)) {
            builder.append("<ul>");
            context.forEach((key, value) -> builder.append("<li><strong>")
                .append(key)
                .append(":</strong> ")
                .append(StringUtils.hasText(value) ? value : "-")
                .append("</li>"));
            builder.append("</ul>");
        } else {
            builder.append("<p>这是一封系统通知邮件。</p>");
        }
        builder.append("<p style=\"color:#888\">发送时间：").append(LocalDateTime.now()).append("</p>");
        builder.append("</div>");
        return builder.toString();
    }
}
