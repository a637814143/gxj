package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64位")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String password,

    @Size(max = 128, message = "姓名长度不能超过128位")
    String fullName,

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128位")
    String email,

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(max = 16, message = "邮箱验证码长度不能超过16位")
    String emailCode,

    String roleCode
) {
}
