package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.CaptchaResponse;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.RefreshTokenRequest;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.UserInfo;
import com.gxj.cropyield.modules.auth.service.AuthService;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final LoginLogService loginLogService;

    public AuthController(AuthService authService, CaptchaService captchaService, LoginLogService loginLogService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.loginLogService = loginLogService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.createCaptcha());
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            return ApiResponse.success(authService.register(request, ipAddress, userAgent));
        } catch (BusinessException ex) {
            loginLogService.record(request.username(), false, ipAddress, userAgent, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            return ApiResponse.success(authService.login(request, ipAddress, userAgent));
        } catch (BusinessException ex) {
            loginLogService.record(request.username(), false, ipAddress, userAgent, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokens> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request.refreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> currentUser() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    @GetMapping("/permissions/check")
    public ApiResponse<Boolean> checkPermission(@RequestParam("role") String roleCode) {
        return ApiResponse.success(authService.hasRole(roleCode));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
