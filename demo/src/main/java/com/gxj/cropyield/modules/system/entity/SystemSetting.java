package com.gxj.cropyield.modules.system.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
/**
 * 系统设置模块的实体类，映射系统设置领域对应的数据表结构。
 */

@Entity
@Table(name = "system_setting")
public class SystemSetting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_region_id")
    private Region defaultRegion;

    @Column(name = "notify_email", length = 128)
    private String notifyEmail;

    @Column(name = "cluster_enabled", nullable = false)
    private boolean clusterEnabled = true;

    @Column(name = "pending_change_count", nullable = false)
    private int pendingChangeCount = 0;

    @Column(name = "security_strategy", length = 64)
    private String securityStrategy;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible = false;

    @Column(name = "public_title", length = 128)
    private String publicTitle;

    @Column(name = "public_summary", length = 512)
    private String publicSummary;

    @Column(name = "public_audience", length = 64)
    private String publicAudience;

    @Column(name = "public_level", length = 32)
    private String publicLevel;

    @Column(name = "public_start_at")
    private java.time.LocalDateTime publicStartAt;

    @Column(name = "public_end_at")
    private java.time.LocalDateTime publicEndAt;

    public Region getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(Region defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public String getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(String notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public int getPendingChangeCount() {
        return pendingChangeCount;
    }

    public void setPendingChangeCount(int pendingChangeCount) {
        this.pendingChangeCount = pendingChangeCount;
    }

    public String getSecurityStrategy() {
        return securityStrategy;
    }

    public void setSecurityStrategy(String securityStrategy) {
        this.securityStrategy = securityStrategy;
    }

    public boolean isPublicVisible() {
        return publicVisible;
    }

    public void setPublicVisible(boolean publicVisible) {
        this.publicVisible = publicVisible;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public void setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
    }

    public String getPublicSummary() {
        return publicSummary;
    }

    public void setPublicSummary(String publicSummary) {
        this.publicSummary = publicSummary;
    }

    public String getPublicAudience() {
        return publicAudience;
    }

    public void setPublicAudience(String publicAudience) {
        this.publicAudience = publicAudience;
    }

    public String getPublicLevel() {
        return publicLevel;
    }

    public void setPublicLevel(String publicLevel) {
        this.publicLevel = publicLevel;
    }

    public java.time.LocalDateTime getPublicStartAt() {
        return publicStartAt;
    }

    public void setPublicStartAt(java.time.LocalDateTime publicStartAt) {
        this.publicStartAt = publicStartAt;
    }

    public java.time.LocalDateTime getPublicEndAt() {
        return publicEndAt;
    }

    public void setPublicEndAt(java.time.LocalDateTime publicEndAt) {
        this.publicEndAt = publicEndAt;
    }
}
