package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.config.JwtProperties;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.common.security.JwtTokenProvider;
import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.UserInfo;
import com.gxj.cropyield.modules.auth.entity.RefreshToken;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.AuthService;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import com.gxj.cropyield.modules.auth.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;
    private final RefreshTokenService refreshTokenService;
    private final LoginLogService loginLogService;

    public AuthServiceImpl(UserRepository userRepository,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           CaptchaService captchaService,
                           RefreshTokenService refreshTokenService,
                           LoginLogService loginLogService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.captchaService = captchaService;
        this.refreshTokenService = refreshTokenService;
        this.loginLogService = loginLogService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        captchaService.validate(request.captchaId(), request.captchaCode());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        RefreshToken refreshToken = refreshTokenService.create(user);
        AuthTokens tokens = buildTokens(authentication, refreshToken.getToken());
        UserInfo userInfo = buildUserInfo(user);

        loginLogService.record(user.getUsername(), true, ipAddress, userAgent, "登录成功");
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

    private AuthTokens buildTokens(Authentication authentication, String refreshToken) {
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return new AuthTokens(accessToken, "Bearer", jwtProperties.getExpiration(), refreshToken);
    }

    private UserInfo buildUserInfo(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toSet());
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            roles
        );
    }

    private Authentication buildAuthentication(User user) {
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(Role::getCode)
            .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
            .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }
}
