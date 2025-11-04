package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象，承载邮箱验证码发送请求参数。
 */

public record EmailCodeRequest(
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128位")
    String email,

    @NotBlank(message = "验证码标识不能为空")
    String captchaId,

    @NotBlank(message = "图形验证码不能为空")
    @Size(max = 16, message = "图形验证码长度不能超过16位")
    String captchaCode
) {
}
