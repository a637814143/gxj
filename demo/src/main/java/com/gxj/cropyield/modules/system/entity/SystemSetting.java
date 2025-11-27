package com.gxj.cropyield.modules.system.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
/**
 * 系统设置模块的实体类，映射系统设置领域对应的数据表结构。
 */

@Entity
@Table(name = "system_setting")
public class SystemSetting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
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
}
