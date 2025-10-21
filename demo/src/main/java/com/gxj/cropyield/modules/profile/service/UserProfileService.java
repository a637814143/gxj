package com.gxj.cropyield.modules.profile.service;

import com.gxj.cropyield.modules.profile.dto.UpdatePasswordRequest;
import com.gxj.cropyield.modules.profile.dto.UpdateUserProfileRequest;
import com.gxj.cropyield.modules.profile.dto.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    void updatePassword(UpdatePasswordRequest request);
}
