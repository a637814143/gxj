package com.gxj.cropyield.modules.consultation.service;

import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.consultation.dto.ConsultationDepartmentOption;
import com.gxj.cropyield.modules.auth.entity.Role;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 农业部门目录，基于系统用户表动态生成咨询部门列表。
 */
@Component
public class ConsultationDepartmentDirectory {

    private static final String ROLE_DEPARTMENT = "AGRICULTURE_DEPT";

    private static final String USER_CODE_PREFIX = "user:";

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
        Map<String, ConsultationDepartmentOption> snapshot = new LinkedHashMap<>();
        List<ConsultationDepartmentOption> options = new ArrayList<>();
        for (User user : users) {
            ConsultationDepartmentOption option = buildOption(determineCode(user), user);
            options.add(option);
            String key = cacheKey(option.code());
            if (key != null) {
                snapshot.put(key, option);
            }
        }
        cache.clear();
        cache.putAll(snapshot);
        return options;
    }

    /**
     * 根据部门编码查找对应的目录项。
     */
    public Optional<ConsultationDepartmentOption> findByCode(String code) {
        String normalized = normalizeDepartmentCode(code);
        if (normalized == null) {
            return Optional.empty();
        }
        String lookupKey = cacheKey(normalized);
        ConsultationDepartmentOption cached = lookupKey == null ? null : cache.get(lookupKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ConsultationDepartmentOption> option = Optional.empty();
        if (isUserScopedCode(normalized)) {
            option = parseUserId(normalized)
                .flatMap(userRepository::findById)
                .filter(this::isDepartmentUser)
                .map(user -> buildOption(normalized, user));
        } else {
            option = userRepository.findByRoleAndDepartment(ROLE_DEPARTMENT, normalized)
                .stream()
                .findFirst()
                .map(user -> buildOption(normalized, user));
        }
        option.ifPresent(value -> {
            String key = cacheKey(value.code());
            if (key != null) {
                cache.put(key, value);
            }
        });
        return option;
    }

    private ConsultationDepartmentOption buildOption(String code, User user) {
        String contactName = StringUtils.hasText(user.getFullName())
            ? user.getFullName().trim()
            : user.getUsername();
        String displayName = StringUtils.hasText(user.getDepartmentCode())
            ? user.getDepartmentCode().trim()
            : contactName;
        String description = String.format("由 %s 提供农业技术支持", contactName);
        return new ConsultationDepartmentOption(code, displayName, description, user.getId(), user.getUsername(), contactName);
    }

    private String determineCode(User user) {
        String departmentCode = normalizeDepartmentCode(user.getDepartmentCode());
        if (departmentCode != null) {
            return departmentCode;
        }
        return USER_CODE_PREFIX + user.getId();
    }

    private String normalizeDepartmentCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return code.trim();
    }

    private String cacheKey(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isUserScopedCode(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        return cacheKey(code).startsWith(USER_CODE_PREFIX);
    }

    private Optional<Long> parseUserId(String code) {
        if (!isUserScopedCode(code)) {
            return Optional.empty();
        }
        String rawId = normalizeDepartmentCode(code).substring(USER_CODE_PREFIX.length());
        try {
            return Optional.of(Long.parseLong(rawId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private boolean isDepartmentUser(User user) {
        return user.getRoles().stream()
            .map(Role::getCode)
            .anyMatch(ROLE_DEPARTMENT::equals);
    }
}
