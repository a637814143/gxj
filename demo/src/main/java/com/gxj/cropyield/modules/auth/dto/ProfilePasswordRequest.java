package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record ProfilePasswordRequest(
    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String currentPassword,

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String newPassword,

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(max = 16, message = "邮箱验证码长度不能超过16位")
    String emailCode
) {
}
