package com.gxj.cropyield.common.mail;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 允许通过单一环境变量提供邮箱账号与授权码。
 * 支持 <code>key253614="user@qq.com:authcode"</code> 的形式，
 * 同时保留对 {@code MAIL_PASSWORD} 的覆盖能力。
 */
@Component
public class
MailCredentialsConfigurator {

    private static final Logger log = LoggerFactory.getLogger(MailCredentialsConfigurator.class);

    private final JavaMailSender mailSender;
    private final String combinedCredential;
    private final String passwordOverride;

    public MailCredentialsConfigurator(JavaMailSender mailSender,
                                       @Value("${spring.mail.username}") String combinedCredential,
                                       @Value("${spring.mail.password}") String passwordOverride) {
        this.mailSender = mailSender;
        this.combinedCredential = StringUtils.trimWhitespace(combinedCredential);
        this.passwordOverride = StringUtils.trimWhitespace(passwordOverride);
    }

    @PostConstruct
    public void applyCredentials() {
        if (!(mailSender instanceof JavaMailSenderImpl senderImpl)) {
            return;
        }

        boolean updated = false;

        if (StringUtils.hasText(combinedCredential) && combinedCredential.contains(":")) {
            String[] parts = combinedCredential.split(":", 2);
            String username = StringUtils.trimWhitespace(parts[0]);
            String derivedPassword = parts.length > 1 ? StringUtils.trimWhitespace(parts[1]) : "";

            if (!StringUtils.hasText(senderImpl.getUsername()) && StringUtils.hasText(username)) {
                senderImpl.setUsername(username);
                updated = true;
            }

            if (StringUtils.hasText(passwordOverride)) {
                senderImpl.setPassword(passwordOverride);
                updated = true;
            } else if (!StringUtils.hasText(senderImpl.getPassword()) && StringUtils.hasText(derivedPassword)) {
                senderImpl.setPassword(derivedPassword);
                updated = true;
            }
        } else {
            if (!StringUtils.hasText(senderImpl.getUsername()) && StringUtils.hasText(combinedCredential)) {
                senderImpl.setUsername(combinedCredential);
                updated = true;
            }

            if (StringUtils.hasText(passwordOverride)) {
                senderImpl.setPassword(passwordOverride);
                updated = true;
            }
        }

        if (updated) {
            log.info("Mail credentials injected from environment");
        }
    }
}

