package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.auth.dto.ProfilePasswordRequest;
import com.gxj.cropyield.modules.auth.dto.ProfileResponse;
import com.gxj.cropyield.modules.auth.dto.ProfileUpdateRequest;
import com.gxj.cropyield.modules.auth.service.ProfileService;
import com.gxj.cropyield.common.web.IpAddressResolver;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 认证与账号模块的控制器，用于暴露认证与账号相关的 REST 接口。
 * <p>核心方法：profile、updateProfile、changePassword。</p>
 */

@RestController
@RequestMapping("/api/auth/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> profile() {
        return ApiResponse.success(profileService.getCurrentProfile());
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(profileService.updateProfile(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ProfilePasswordRequest request) {
        profileService.changePassword(request);
        return ApiResponse.success();
    }

    @GetMapping("/email-code")
    public ApiResponse<Void> sendPasswordEmailCode(HttpServletRequest request) {
        String ipAddress = IpAddressResolver.resolve(request);
        profileService.sendPasswordChangeCode(ipAddress);
        return ApiResponse.success();
    }
}
