package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ForecastModelDepartmentPolicy;
import java.util.Set;

public record ForecastModelPolicyResponse(
        Long id,
        String departmentCode,
        Set<ForecastModel.ModelType> allowedTypes,
        boolean canManage
) {
    public static ForecastModelPolicyResponse fromEntity(ForecastModelDepartmentPolicy policy) {
        return new ForecastModelPolicyResponse(
                policy.getId(),
                policy.getDepartmentCode(),
                policy.toAllowedTypeSet(),
                Boolean.TRUE.equals(policy.getCanManage())
        );
    }
}
