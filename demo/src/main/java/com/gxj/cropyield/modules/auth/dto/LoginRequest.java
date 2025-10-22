package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record LoginRequest(
    @NotBlank(message = "用户名不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password,

    @NotBlank(message = "验证码标识不能为空")
    String captchaId,

    @NotBlank(message = "验证码不能为空")
    String captchaCode
) {
}
