package com.gxj.cropyield.modules.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
    @NotBlank(message = "请输入当前密码")
    String currentPassword,

    @NotBlank(message = "请输入新密码")
    @Size(min = 8, message = "新密码长度不能少于 8 位")
    String newPassword
) {
}
