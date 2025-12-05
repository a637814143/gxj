package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.auth.constant.SystemRole;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.service.CurrentUserService;
import com.gxj.cropyield.modules.forecast.config.ForecastModelSettings;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelPolicyRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelPolicyResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ForecastModelDepartmentPolicy;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelDepartmentPolicyRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastModelAccessService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ForecastModelAccessServiceImpl implements ForecastModelAccessService {

    private final ForecastModelDepartmentPolicyRepository policyRepository;
    private final ForecastModelSettings forecastModelSettings;
    private final CurrentUserService currentUserService;

    public ForecastModelAccessServiceImpl(ForecastModelDepartmentPolicyRepository policyRepository,
                                          ForecastModelSettings forecastModelSettings,
                                          CurrentUserService currentUserService) {
        this.policyRepository = policyRepository;
        this.forecastModelSettings = forecastModelSettings;
        this.currentUserService = currentUserService;
    }

    @Override
    public Set<ForecastModel.ModelType> getEffectiveAllowedTypesForUser(User user) {
        Set<ForecastModel.ModelType> globalAllowed = EnumSet.copyOf(forecastModelSettings.getAllowedTypes());
        if (hasRole(user, SystemRole.ADMIN.getCode())) {
            return globalAllowed;
        }
        if (user == null || !StringUtils.hasText(user.getDepartmentCode())) {
            return globalAllowed;
        }

        return policyRepository.findByDepartmentCodeIgnoreCase(user.getDepartmentCode().trim())
                .map(policy -> {
                    Set<ForecastModel.ModelType> departmentAllowed = policy.toAllowedTypeSet();
                    if (departmentAllowed.isEmpty()) {
                        return globalAllowed;
                    }
                    return departmentAllowed.stream()
                            .filter(globalAllowed::contains)
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ForecastModel.ModelType.class)));
                })
                .orElse(globalAllowed);
    }

    @Override
    public Set<ForecastModel.ModelType> getEffectiveAllowedTypesForCurrentUser() {
        return getEffectiveAllowedTypesForUser(currentUserService.getCurrentUser());
    }

    @Override
    public boolean canCurrentUserManageModels() {
        return canUserManageModels(currentUserService.getCurrentUser());
    }

    @Override
    public boolean canUserManageModels(User user) {
        if (user == null) {
            return false;
        }
        if (hasRole(user, SystemRole.ADMIN.getCode())) {
            return true;
        }
        if (!hasRole(user, SystemRole.AGRICULTURE_DEPT.getCode())) {
            return false;
        }
        if (!StringUtils.hasText(user.getDepartmentCode())) {
            return false;
        }
        return policyRepository.findByDepartmentCodeIgnoreCase(user.getDepartmentCode().trim())
                .map(policy -> Boolean.TRUE.equals(policy.getCanManage()))
                .orElse(false);
    }

    @Override
    public List<ForecastModelPolicyResponse> listPolicies() {
        return policyRepository.findAll().stream()
                .sorted(Comparator.comparing(ForecastModelDepartmentPolicy::getDepartmentCode, String.CASE_INSENSITIVE_ORDER))
                .map(ForecastModelPolicyResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public List<ForecastModelPolicyResponse> savePolicies(List<ForecastModelPolicyRequest> requests) {
        List<ForecastModelPolicyRequest> safeRequests = requests == null ? List.of() : requests;
        List<ForecastModelDepartmentPolicy> existing = policyRepository.findAll();
        Map<String, ForecastModelDepartmentPolicy> existingByCode = new HashMap<>();
        for (ForecastModelDepartmentPolicy policy : existing) {
            if (StringUtils.hasText(policy.getDepartmentCode())) {
                existingByCode.put(policy.getDepartmentCode().trim().toLowerCase(), policy);
            }
        }

        Set<String> incomingCodes = new HashSet<>();
        List<ForecastModelDepartmentPolicy> toSave = new ArrayList<>();

        for (ForecastModelPolicyRequest request : safeRequests) {
            if (request == null || !StringUtils.hasText(request.departmentCode())) {
                continue;
            }
            String normalizedCode = request.departmentCode().trim();
            String lookup = normalizedCode.toLowerCase();
            incomingCodes.add(lookup);

            ForecastModelDepartmentPolicy target = existingByCode.getOrDefault(lookup, new ForecastModelDepartmentPolicy());
            target.setDepartmentCode(normalizedCode);
            target.setAllowedTypes(filterAllowedTypes(request.allowedTypes()));
            target.setCanManage(request.canManage());
            toSave.add(target);
        }

        List<ForecastModelDepartmentPolicy> toDelete = existing.stream()
                .filter(policy -> StringUtils.hasText(policy.getDepartmentCode()))
                .filter(policy -> !incomingCodes.contains(policy.getDepartmentCode().trim().toLowerCase()))
                .toList();

        if (!toDelete.isEmpty()) {
            policyRepository.deleteAll(toDelete);
        }

        if (!toSave.isEmpty()) {
            policyRepository.saveAll(toSave);
        }

        return listPolicies();
    }

    private Set<ForecastModel.ModelType> filterAllowedTypes(Set<ForecastModel.ModelType> candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return Set.of();
        }
        Set<ForecastModel.ModelType> globalAllowed = forecastModelSettings.getAllowedTypes();
        return candidate.stream()
                .filter(Objects::nonNull)
                .filter(globalAllowed::contains)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ForecastModel.ModelType.class)));
    }

    private boolean hasRole(User user, String roleCode) {
        if (!StringUtils.hasText(roleCode) || user.getRoles() == null) {
            return false;
        }
        for (Role role : user.getRoles()) {
            if (role != null && role.getCode() != null && role.getCode().equalsIgnoreCase(roleCode)) {
                return true;
            }
        }
        return false;
    }
}
