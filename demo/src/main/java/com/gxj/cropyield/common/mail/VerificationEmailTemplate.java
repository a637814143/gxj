package com.gxj.cropyield.common.mail;

import org.springframework.stereotype.Component;

import java.time.Duration;
/**
 * 公共邮件模块的模板组件，为邮件验证码提供统一的文案与样式。
 */

@Component
public class VerificationEmailTemplate {

    public String subject() {
        return "作物产量平台邮箱验证码";
    }

    public String buildHtmlContent(String code, Duration ttl) {
        long minutes = Math.max(ttl.toMinutes(), 1);
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.6\">" +
            "<h2>验证码通知</h2>" +
            "<p>您好！感谢注册作物产量平台。</p>" +
            "<p>您的验证码为：<strong style=\"font-size:18px\">" + code + "</strong></p>" +
            "<p>该验证码在" + minutes + "分钟内有效，请勿泄露给他人。</p>" +
            "<p>如果非本人操作，请忽略本邮件。</p>" +
            "<p style=\"color:#888\">此邮件为系统自动发送，请勿直接回复。</p>" +
            "</div>";
    }
}
