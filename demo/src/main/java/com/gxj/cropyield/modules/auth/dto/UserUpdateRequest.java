package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserUpdateRequest(
    @Size(max = 128, message = "姓名长度不能超过128位")
    String fullName,

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128位")
    String email,

    Set<Long> roleIds
) {
}
