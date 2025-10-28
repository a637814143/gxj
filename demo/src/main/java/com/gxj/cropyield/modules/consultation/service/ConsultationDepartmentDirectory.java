package com.gxj.cropyield.modules.consultation.service;

import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.consultation.dto.ConsultationDepartmentOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 农业部门目录，基于系统用户表动态生成咨询部门列表。
 */
@Component
public class ConsultationDepartmentDirectory {

    private static final String ROLE_DEPARTMENT = "AGRICULTURE_DEPT";

    private final UserRepository userRepository;
    private final Map<String, ConsultationDepartmentOption> cache = new ConcurrentHashMap<>();

    public ConsultationDepartmentDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 返回当前可用的咨询部门列表（按照部门编码去重，保持数据库中的顺序）。
     */
    public List<ConsultationDepartmentOption> listActiveDepartments() {
        List<User> users = userRepository.findDepartmentUsers(ROLE_DEPARTMENT);
        Map<String, ConsultationDepartmentOption> ordered = new LinkedHashMap<>();
        for (User user : users) {
            String departmentCode = normalizeDepartmentCode(user.getDepartmentCode());
            if (departmentCode == null || ordered.containsKey(departmentCode)) {
                continue;
            }
            ConsultationDepartmentOption option = buildOption(departmentCode, user);
            ordered.put(departmentCode, option);
        }
        cache.putAll(ordered);
        return new ArrayList<>(ordered.values());
    }

    /**
     * 根据部门编码查找对应的目录项。
     */
    public Optional<ConsultationDepartmentOption> findByCode(String code) {
        String normalized = normalizeDepartmentCode(code);
        if (normalized == null) {
            return Optional.empty();
        }
        ConsultationDepartmentOption cached = cache.get(normalized);
        if (cached != null) {
            return Optional.of(cached);
        }
        return userRepository.findByRoleAndDepartment(ROLE_DEPARTMENT, normalized)
            .stream()
            .findFirst()
            .map(user -> {
                ConsultationDepartmentOption option = buildOption(normalized, user);
                cache.put(normalized, option);
                return option;
            });
    }

    private ConsultationDepartmentOption buildOption(String code, User user) {
        String contactName = user.getFullName();
        if (!StringUtils.hasText(contactName)) {
            contactName = user.getUsername();
        }
        String displayName = user.getDepartmentCode();
        if (!StringUtils.hasText(displayName)) {
            displayName = contactName;
        }
        String description = String.format("由 %s 提供农业技术支持", contactName);
        return new ConsultationDepartmentOption(code, displayName, description, user.getId(), user.getUsername(), contactName);
    }

    private String normalizeDepartmentCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return code.trim();
    }
}
