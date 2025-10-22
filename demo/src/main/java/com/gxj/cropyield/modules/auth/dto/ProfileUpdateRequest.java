package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record ProfileUpdateRequest(
    @Size(max = 128, message = "姓名长度不能超过128个字符")
    String fullName,

    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    @Email(message = "请输入有效的邮箱地址")
    String email
) {
}
