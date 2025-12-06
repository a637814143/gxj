package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record UserPasswordRequest(
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String newPassword
) {
}
