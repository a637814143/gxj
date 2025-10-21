package com.gxj.cropyield.modules.auth.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_profile")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "security_settings", columnDefinition = "TEXT")
    private String securitySettings;

    @Column(name = "business_info", columnDefinition = "TEXT")
    private String businessInfo;

    @Column(name = "personalization_settings", columnDefinition = "TEXT")
    private String personalizationSettings;

    @Column(name = "data_operations", columnDefinition = "TEXT")
    private String dataOperations;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSecuritySettings() {
        return securitySettings;
    }

    public void setSecuritySettings(String securitySettings) {
        this.securitySettings = securitySettings;
    }

    public String getBusinessInfo() {
        return businessInfo;
    }

    public void setBusinessInfo(String businessInfo) {
        this.businessInfo = businessInfo;
    }

    public String getPersonalizationSettings() {
        return personalizationSettings;
    }

    public void setPersonalizationSettings(String personalizationSettings) {
        this.personalizationSettings = personalizationSettings;
    }

    public String getDataOperations() {
        return dataOperations;
    }

    public void setDataOperations(String dataOperations) {
        this.dataOperations = dataOperations;
    }
}
