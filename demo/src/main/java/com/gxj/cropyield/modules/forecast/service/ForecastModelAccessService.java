package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelPolicyRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelPolicyResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.List;
import java.util.Set;

public interface ForecastModelAccessService {

    Set<ForecastModel.ModelType> getEffectiveAllowedTypesForUser(User user);

    Set<ForecastModel.ModelType> getEffectiveAllowedTypesForCurrentUser();

    boolean canCurrentUserManageModels();

    boolean canUserManageModels(User user);

    List<ForecastModelPolicyResponse> listPolicies();

    List<ForecastModelPolicyResponse> savePolicies(List<ForecastModelPolicyRequest> requests);
}
