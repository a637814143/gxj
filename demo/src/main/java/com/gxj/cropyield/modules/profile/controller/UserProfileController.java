package com.gxj.cropyield.modules.profile.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.profile.dto.UpdatePasswordRequest;
import com.gxj.cropyield.modules.profile.dto.UpdateUserProfileRequest;
import com.gxj.cropyield.modules.profile.dto.UserProfileResponse;
import com.gxj.cropyield.modules.profile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUserProfile() {
        return ApiResponse.success(userProfileService.getCurrentUserProfile());
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userProfileService.updateCurrentUserProfile(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userProfileService.updatePassword(request);
        return ApiResponse.success();
    }
}
