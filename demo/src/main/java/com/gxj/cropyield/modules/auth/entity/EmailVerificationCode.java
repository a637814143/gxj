package com.gxj.cropyield.modules.auth.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
/**
 * 认证与账号模块的实体类，记录邮箱验证码的生成与验证情况。
 */

@Entity
@Table(name = "auth_email_verification_code",
    indexes = {
        @Index(name = "idx_auth_email", columnList = "email"),
        @Index(name = "idx_auth_ip_created", columnList = "requested_ip, created_at"),
        @Index(name = "idx_auth_expires", columnList = "expires_at")
    })
public class EmailVerificationCode extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String email;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getRequestedIp() {
        return requestedIp;
    }

    public void setRequestedIp(String requestedIp) {
        this.requestedIp = requestedIp;
    }
}
