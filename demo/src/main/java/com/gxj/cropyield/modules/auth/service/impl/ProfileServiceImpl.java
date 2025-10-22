package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.ProfilePasswordRequest;
import com.gxj.cropyield.modules.auth.dto.ProfileResponse;
import com.gxj.cropyield.modules.auth.dto.ProfileUpdateRequest;
import com.gxj.cropyield.modules.auth.dto.RoleSummary;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.ProfileService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile() {
        User user = loadCurrentUser();
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = loadCurrentUser();

        if (request.fullName() != null) {
            String trimmedFullName = request.fullName().trim();
            user.setFullName(trimmedFullName.isEmpty() ? null : trimmedFullName);
        }

        if (request.email() != null) {
            String trimmedEmail = request.email().trim();
            user.setEmail(trimmedEmail.isEmpty() ? null : trimmedEmail);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(ProfilePasswordRequest request) {
        User user = loadCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User loadCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }

        String username = authentication.getName();
        return userRepository.findWithRolesByUsername(username)
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
    }

    private ProfileResponse toResponse(User user) {
        Set<RoleSummary> roles = user.getRoles().stream()
            .sorted(Comparator.comparing(Role::getId))
            .map(role -> new RoleSummary(role.getId(), role.getCode(), role.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            roles,
            user.getCreatedAt()
        );
    }
}
