package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.CaptchaResponse;
/**
 * 认证与账号模块的业务接口（接口），定义认证与账号相关的核心业务操作。
 * <p>核心方法：createCaptcha、validate。</p>
 */

public interface CaptchaService {

    CaptchaResponse createCaptcha();

    void validate(String captchaId, String captchaCode);
}
