package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.CurrentUserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证与账号模块的业务实现类，负责提供当前登录用户相关的上下文信息。
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        String username = authentication.getName();
        return userRepository.findWithRolesByUsername(username)
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
    }

    @Override
    public Set<String> getCurrentUserRoleCodes() {
        return getCurrentUser().getRoles().stream()
            .map(role -> role.getCode() == null ? "" : role.getCode())
            .filter(code -> !code.isBlank())
            .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        return getCurrentUserRoleCodes().contains(roleCode);
    }
}
