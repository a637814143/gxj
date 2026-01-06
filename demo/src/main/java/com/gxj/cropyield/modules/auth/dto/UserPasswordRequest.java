package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 * 注意：管理员重置密码时，newPassword 字段可以为空，后端会自动生成默认密码（用户名+123456）
 */

public record UserPasswordRequest(
    @Size(min = 6, max = 128, message = "密码长度需要在6-128位之间")
    String newPassword
) {
}
