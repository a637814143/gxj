package com.gxj.cropyield.modules.auth.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
/**
 * 认证与账号模块的实体类，映射认证与账号领域对应的数据表结构。
 */

@Entity
@Table(name = "sys_login_log")
public class LoginLog extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 256)
    private String userAgent;

    @Column(length = 256)
    private String message;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
