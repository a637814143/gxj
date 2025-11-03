package com.gxj.cropyield.common.mail;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
/**
 * 公共邮件模块的基础组件，封装邮件发送的底层细节。
 */

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String defaultFrom;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${spring.mail.from:no-reply@cropyield.local}") String defaultFrom) {
        this.mailSender = mailSender;
        this.defaultFrom = defaultFrom;
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendHtmlAsync(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(defaultFrom);
            mailSender.send(message);
            return CompletableFuture.completedFuture(null);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new BusinessException(ResultCode.SERVER_ERROR, "邮件发送失败"));
            return future;
        }
    }
}
