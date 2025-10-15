package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.CaptchaResponse;

public interface CaptchaService {

    CaptchaResponse createCaptcha();

    void validate(String captchaId, String captchaCode);
}
