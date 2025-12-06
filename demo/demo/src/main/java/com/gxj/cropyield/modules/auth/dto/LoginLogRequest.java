package com.gxj.cropyield.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record LoginLogRequest(
    @NotBlank(message = "请输入用户名")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    String username,

    @NotNull(message = "请选择登录结果")
    Boolean success,

    @Size(max = 64, message = "IP 地址长度不能超过64个字符")
    String ipAddress,

    @Size(max = 256, message = "User-Agent 长度不能超过256个字符")
    String userAgent,

    @Size(max = 256, message = "描述信息长度不能超过256个字符")
    String message
) {
}
