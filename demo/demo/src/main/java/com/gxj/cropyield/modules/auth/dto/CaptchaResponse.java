package com.gxj.cropyield.modules.auth.dto;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record CaptchaResponse(
    String captchaId,
    String imageBase64
) {
}
