package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.logging.AuditLogger;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.common.security.JwtTokenProvider;
import com.gxj.cropyield.modules.auth.constant.SystemRole;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.ResetPasswordRequest;
import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RoleRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogger auditLogger;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogger = auditLogger;
    }

    @Override
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(user -> new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
            ));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        validateUsernameAvailable(request.username());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllById(request.roleIds()).stream().collect(Collectors.toSet());
            user.setRoles(roles);
        } else {
            Role defaultRole = roleRepository.findByCode(SystemRole.FARMER.getCode())
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "默认角色不存在"));
            user.setRoles(Collections.singleton(defaultRole));
        }

        User saved = userRepository.save(user);
        auditLogger.record("创建用户", "管理员创建用户:" + saved.getUsername());
        return buildUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validateUsernameAvailable(request.username());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());

        String roleCode = request.roleCode();
        Role role = roleRepository.findByCode(resolveRoleCode(roleCode))
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "角色不存在"));
        user.setRoles(Collections.singleton(role));

        User saved = userRepository.save(user);
        auditLogger.recordForUser(saved.getUsername(), "用户注册", "新用户注册成功");
        return buildUserResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        Set<String> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            roleCodes.stream().map(code -> new SimpleGrantedAuthority("ROLE_" + code)).collect(Collectors.toSet())
        );

        String token = jwtTokenProvider.generateToken(authenticationToken);
        auditLogger.recordForUser(user.getUsername(), "用户登录", "登录成功");
        return new LoginResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            token,
            roleCodes
        );
    }

    @Override
    @Transactional
    public void resetPassword(String username, ResetPasswordRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogger.record("密码重置", "用户更新了密码");
    }

    private void validateUsernameAvailable(String username) {
        userRepository.findByUsername(username)
            .ifPresent(user -> {
                throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
            });
    }

    private String resolveRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return SystemRole.FARMER.getCode();
        }
        return roleCode;
    }

    private UserResponse buildUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
            user.getCreatedAt()
        );
    }
}
