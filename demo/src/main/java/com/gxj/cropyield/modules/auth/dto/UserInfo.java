package com.gxj.cropyield.modules.auth.dto;

import java.util.Set;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record UserInfo(
    Long id,
    String username,
    String fullName,
    String email,
    Set<String> roles,
    String departmentCode,
    String departmentName
) {
}
