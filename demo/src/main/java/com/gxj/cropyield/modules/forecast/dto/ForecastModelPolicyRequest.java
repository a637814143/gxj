package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record ForecastModelPolicyRequest(
        @NotBlank(message = "部门编码不能为空") String departmentCode,
        Set<ForecastModel.ModelType> allowedTypes,
        boolean canManage
) {
}
