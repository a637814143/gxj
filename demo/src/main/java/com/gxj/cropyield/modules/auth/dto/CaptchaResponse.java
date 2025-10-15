package com.gxj.cropyield.modules.auth.dto;

public record CaptchaResponse(
    String captchaId,
    String imageBase64
) {
}
