package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordRequest(
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String newPassword
) {
}
