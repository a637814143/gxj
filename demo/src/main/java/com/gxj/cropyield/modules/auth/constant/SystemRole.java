package com.gxj.cropyield.modules.auth.constant;
/**
 * 认证与账号模块的核心类（枚举），提供认证与账号相关的支撑能力。
 */

public enum SystemRole {
    ADMIN("ADMIN", "系统管理员"),
    AGRICULTURE_DEPT("AGRICULTURE_DEPT", "农业部门用户"),
    FARMER("FARMER", "企业/农户用户");

    private final String code;
    private final String displayName;

    SystemRole(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
