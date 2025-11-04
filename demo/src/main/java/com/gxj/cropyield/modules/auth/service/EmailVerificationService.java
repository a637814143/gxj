package com.gxj.cropyield.modules.auth.service;
/**
 * 认证与账号模块的业务接口，定义邮箱验证码的核心操作。
 */

public interface EmailVerificationService {

    void sendVerificationCode(String email, String captchaId, String captchaCode, String ipAddress);

    void validateAndConsume(String email, String code);
}
