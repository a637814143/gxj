package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门级模型使用与管理权限控制。
 */
@Entity
@Table(name = "forecast_model_department_policy")
public class ForecastModelDepartmentPolicy extends BaseEntity {

    @Column(name = "department_code", nullable = false, unique = true, length = 64)
    private String departmentCode;

    @Column(name = "allowed_types", length = 512)
    private String allowedTypes;

    @Column(name = "can_manage", nullable = false)
    private Boolean canManage = Boolean.FALSE;

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(Set<ForecastModel.ModelType> allowedTypes) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            this.allowedTypes = null;
            return;
        }
        this.allowedTypes = allowedTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    public Set<ForecastModel.ModelType> toAllowedTypeSet() {
        if (allowedTypes == null || allowedTypes.isBlank()) {
            return Collections.emptySet();
        }
        Set<ForecastModel.ModelType> result = EnumSet.noneOf(ForecastModel.ModelType.class);
        for (String token : allowedTypes.split(",")) {
            try {
                result.add(ForecastModel.ModelType.valueOf(token.trim()));
            } catch (IllegalArgumentException ignored) {
                // 跳过无效值，保持兼容
            }
        }
        return result;
    }

    public Boolean getCanManage() {
        return canManage;
    }

    public void setCanManage(Boolean canManage) {
        this.canManage = canManage;
    }
}
