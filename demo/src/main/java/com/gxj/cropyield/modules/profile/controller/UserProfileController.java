package com.gxj.cropyield.modules.profile.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.profile.dto.UpdatePasswordRequest;
import com.gxj.cropyield.modules.profile.service.UserProfileService;
import jakarta.validation.Valid;
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

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userProfileService.updatePassword(request);
        return ApiResponse.success();
    }
}
