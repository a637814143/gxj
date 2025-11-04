package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.mail.EmailSender;
import com.gxj.cropyield.common.mail.VerificationEmailTemplate;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.EmailVerificationCode;
import com.gxj.cropyield.modules.auth.repository.EmailVerificationCodeRepository;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import com.gxj.cropyield.modules.auth.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 * 认证与账号模块的业务实现类，负责邮箱验证码的生成、发送与核验逻辑。
 */

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofMinutes(1);
    private static final Duration IP_RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_IP_REQUESTS = 10;

    private final EmailVerificationCodeRepository repository;
    private final CaptchaService captchaService;
    private final EmailSender emailSender;
    private final VerificationEmailTemplate emailTemplate;

    public EmailVerificationServiceImpl(EmailVerificationCodeRepository repository,
                                        CaptchaService captchaService,
                                        EmailSender emailSender,
                                        VerificationEmailTemplate emailTemplate) {
        this.repository = repository;
        this.captchaService = captchaService;
        this.emailSender = emailSender;
        this.emailTemplate = emailTemplate;
    }

    @Override
    @Transactional
    public void sendVerificationCode(String email, String captchaId, String captchaCode, String ipAddress) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱不能为空");
        }
        captchaService.validate(captchaId, captchaCode);

        LocalDateTime now = LocalDateTime.now();
        repository.findTopByEmailOrderByCreatedAtDesc(email)
            .ifPresent(latest -> {
                if (Duration.between(latest.getCreatedAt(), now).compareTo(MIN_REQUEST_INTERVAL) < 0) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
                }
            });

        if (StringUtils.hasText(ipAddress)) {
            long recentCount = repository.countByRequestedIpAndCreatedAtAfter(ipAddress, now.minus(IP_RATE_LIMIT_WINDOW));
            if (recentCount >= MAX_IP_REQUESTS) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "当前请求过于频繁，请稍后再试");
            }
        }

        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setEmail(email);
        entity.setCode(generateCode());
        entity.setExpiresAt(now.plus(CODE_TTL));
        entity.setRequestedIp(ipAddress);
        repository.save(entity);

        String subject = emailTemplate.subject();
        String html = emailTemplate.buildHtmlContent(entity.getCode(), CODE_TTL);
        try {
            emailSender.sendHtmlAsync(email, subject, html).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("发送邮箱验证码失败: {}", ex.getMessage(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void validateAndConsume(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱验证码不能为空");
        }

        EmailVerificationCode verification = repository.findTopByEmailAndCodeOrderByCreatedAtDesc(email, code)
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "验证码不正确"));

        if (verification.isUsed()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已被使用");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期");
        }

        verification.setUsed(true);
        repository.save(verification);
    }

    @Transactional
    @Scheduled(cron = "0 0/30 * * * ?")
    public void purgeExpired() {
        LocalDateTime threshold = LocalDateTime.now();
        repository.deleteByExpiresAtBefore(threshold.minusDays(1));
    }

    private String generateCode() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return Integer.toString(value);
    }
}
