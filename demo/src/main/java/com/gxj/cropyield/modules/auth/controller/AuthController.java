package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ApiResponse;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        String ipAddress = resolveClientIp(httpRequest);
        emailVerificationService.sendVerificationCode(request.email(), request.captchaId(), request.captchaCode(), ipAddress);
        return ApiResponse.success();
    }

    @PostMapping("/password/reset-code")
    public ApiResponse<Void> passwordResetCode(@Valid @RequestBody PasswordResetCodeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = resolveClientIp(httpRequest);
        passwordResetService.sendResetCode(request, ipAddress);
        return ApiResponse.success();
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> passwordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        passwordResetService.resetPassword(request, ipAddress, userAgent);
        return ApiResponse.success();
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

    @PostMapping("/login/admin")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = resolveClientIp(httpRequest);
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
        String ipAddress = resolveClientIp(httpRequest);
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

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String headerValue = request.getHeader(header);
            if (StringUtils.hasText(headerValue) && !"unknown".equalsIgnoreCase(headerValue)) {
                String candidate = headerValue.split(",")[0].trim();
                String normalized = normalizeIp(candidate);
                if (StringUtils.hasText(normalized)) {
                    return normalized;
                }
            }
        }

        return normalizeIp(request.getRemoteAddr());
    }

    private String normalizeIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return null;
        }

        String trimmed = ipAddress.trim();
        if ("0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            return "127.0.0.1";
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(trimmed);
            if (inetAddress.isLoopbackAddress()) {
                return "127.0.0.1";
            }
            if (inetAddress instanceof Inet4Address) {
                return inetAddress.getHostAddress();
            }
            if (inetAddress instanceof Inet6Address) {
                String hostAddress = inetAddress.getHostAddress();
                int mappedIndex = hostAddress.lastIndexOf(":");
                if (mappedIndex != -1) {
                    String possibleIpv4 = hostAddress.substring(mappedIndex + 1);
                    if (possibleIpv4.contains(".")) {
                        return possibleIpv4;
                    }
                }
                return hostAddress;
            }
            return inetAddress.getHostAddress();
        } catch (UnknownHostException ex) {
            return trimmed;
        }
    }
}
