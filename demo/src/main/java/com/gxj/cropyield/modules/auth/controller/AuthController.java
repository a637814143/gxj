package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.web.IpAddressResolver;
import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.CaptchaResponse;
import com.gxj.cropyield.modules.auth.dto.EmailCodeRequest;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.PasswordResetCodeRequest;
import com.gxj.cropyield.modules.auth.dto.PasswordResetRequest;
import com.gxj.cropyield.modules.auth.dto.RefreshTokenRequest;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.UserInfo;
import com.gxj.cropyield.modules.auth.service.AuthService;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import com.gxj.cropyield.modules.auth.service.EmailVerificationService;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import com.gxj.cropyield.modules.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 认证与账号模块的控制器，用于暴露认证与账号相关的 REST 接口。
 * <p>核心方法：captcha、register、login、adminLogin、userLogin、refreshToken、currentUser、checkPermission。</p>
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final EmailVerificationService emailVerificationService;
    private final LoginLogService loginLogService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          CaptchaService captchaService,
                          EmailVerificationService emailVerificationService,
                          LoginLogService loginLogService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.emailVerificationService = emailVerificationService;
        this.loginLogService = loginLogService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.createCaptcha());
    }

    @PostMapping("/email-code")
    public ApiResponse<Void> emailCode(@Valid @RequestBody EmailCodeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        emailVerificationService.sendVerificationCode(request.email(), request.captchaId(), request.captchaCode(), ipAddress);
        return ApiResponse.success();
    }

    @PostMapping("/password/reset-code")
    public ApiResponse<Void> passwordResetCode(@Valid @RequestBody PasswordResetCodeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        passwordResetService.sendResetCode(request, ipAddress);
        return ApiResponse.success();
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> passwordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        passwordResetService.resetPassword(request, ipAddress, userAgent);
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
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
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            return ApiResponse.success(authService.login(request, ipAddress, userAgent));
        } catch (BusinessException ex) {
            loginLogService.record(request.username(), false, ipAddress, userAgent, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/login/admin")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            return ApiResponse.success(authService.loginAsAdmin(request, ipAddress, userAgent));
        } catch (BusinessException ex) {
            loginLogService.record(request.username(), false, ipAddress, userAgent, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/login/user")
    public ApiResponse<LoginResponse> userLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            return ApiResponse.success(authService.loginAsUser(request, ipAddress, userAgent));
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

}
