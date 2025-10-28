package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.config.JwtProperties;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.common.security.JwtTokenProvider;
import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.UserInfo;
import com.gxj.cropyield.modules.auth.entity.RefreshToken;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RoleRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.AuthService;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import com.gxj.cropyield.modules.auth.service.RefreshTokenService;
import com.gxj.cropyield.modules.auth.constant.SystemRole;
import com.gxj.cropyield.modules.consultation.dto.ConsultationDepartmentOption;
import com.gxj.cropyield.modules.consultation.service.ConsultationDepartmentDirectory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 认证与账号模块的业务实现类，负责落实认证与账号领域的业务处理逻辑。
 * <p>核心方法：login、performLogin、loginAsAdmin、loginAsUser、register、refreshToken、buildTokens、getCurrentUser。</p>
 */

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;
    private final RefreshTokenService refreshTokenService;
    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final ConsultationDepartmentDirectory departmentDirectory;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           CaptchaService captchaService,
                           RefreshTokenService refreshTokenService,
                           LoginLogService loginLogService,
                           PasswordEncoder passwordEncoder,
                           ConsultationDepartmentDirectory departmentDirectory) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.captchaService = captchaService;
        this.refreshTokenService = refreshTokenService;
        this.loginLogService = loginLogService;
        this.passwordEncoder = passwordEncoder;
        this.departmentDirectory = departmentDirectory;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        return performLogin(request, ipAddress, userAgent, null, null, "登录成功");
    }

    @Override
    @Transactional
    public LoginResponse loginAsAdmin(LoginRequest request, String ipAddress, String userAgent) {
        return performLogin(
            request,
            ipAddress,
            userAgent,
            EnumSet.of(SystemRole.ADMIN, SystemRole.AGRICULTURE_DEPT),
            "该账号无权使用管理员登录",
            "管理员登录成功"
        );
    }

    @Override
    @Transactional
    public LoginResponse loginAsUser(LoginRequest request, String ipAddress, String userAgent) {
        return performLogin(
            request,
            ipAddress,
            userAgent,
            EnumSet.of(SystemRole.FARMER),
            "该账号无权使用用户登录",
            "用户登录成功"
        );
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        userRepository.findByUsername(request.username())
            .ifPresent(user -> {
                throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
            });

        Role role = resolveRole(request.roleCode());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());

        var roles = new HashSet<Role>();
        roles.add(role);
        user.setRoles(roles);

        User saved = userRepository.saveAndFlush(user);

        Authentication authentication = buildAuthentication(saved);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken refreshToken = refreshTokenService.create(saved);
        AuthTokens tokens = buildTokens(authentication, refreshToken.getToken());
        UserInfo userInfo = buildUserInfo(saved);

        loginLogService.record(saved.getUsername(), true, ipAddress, userAgent, "注册成功");
        return new LoginResponse(tokens, userInfo);
    }

    @Override
    @Transactional
    public AuthTokens refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validate(refreshTokenValue);
        User user = refreshToken.getUser();
        Authentication authentication = buildAuthentication(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        RefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return buildTokens(authentication, rotated.getToken());
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return userRepository.findByUsername(authentication.getName())
            .map(this::buildUserInfo)
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleCode));
    }

    private LoginResponse performLogin(LoginRequest request,
                                        String ipAddress,
                                        String userAgent,
                                        Set<SystemRole> allowedRoles,
                                        String forbiddenMessage,
                                        String successMessage) {
        captchaService.validate(request.captchaId(), request.captchaCode());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        if (allowedRoles != null && !allowedRoles.isEmpty()) {
            Set<String> allowedCodes = allowedRoles.stream()
                .map(SystemRole::getCode)
                .collect(Collectors.toSet());
            boolean hasAllowedRole = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(allowedCodes::contains);
            if (!hasAllowedRole) {
                throw new BusinessException(ResultCode.FORBIDDEN,
                    forbiddenMessage != null ? forbiddenMessage : "当前账号没有访问权限");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken refreshToken = refreshTokenService.create(user);
        AuthTokens tokens = buildTokens(authentication, refreshToken.getToken());
        UserInfo userInfo = buildUserInfo(user);

        loginLogService.record(user.getUsername(), true, ipAddress, userAgent,
            successMessage != null ? successMessage : "登录成功");
        return new LoginResponse(tokens, userInfo);
    }

    private AuthTokens buildTokens(Authentication authentication, String refreshToken) {
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return new AuthTokens(accessToken, "Bearer", jwtProperties.getExpiration(), refreshToken);
    }

    private UserInfo buildUserInfo(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toSet());
        String departmentCode = user.getDepartmentCode();
        String departmentName = departmentDirectory.findByCode(departmentCode)
            .map(ConsultationDepartmentOption::name)
            .orElseGet(() -> StringUtils.hasText(departmentCode) ? departmentCode.trim() : null);
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            roles,
            departmentCode,
            departmentName
        );
    }

    private Authentication buildAuthentication(User user) {
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(Role::getCode)
            .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
            .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }

    private Role resolveRole(String roleCode) {
        String normalizedRoleCode = roleCode == null ? null : roleCode.trim();
        String effectiveRoleCode = (normalizedRoleCode == null || normalizedRoleCode.isEmpty())
            ? SystemRole.FARMER.getCode()
            : normalizedRoleCode;
        return roleRepository.findByCode(effectiveRoleCode)
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "角色不存在"));
    }
}
