package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.ProfilePasswordRequest;
import com.gxj.cropyield.modules.auth.dto.ProfileResponse;
import com.gxj.cropyield.modules.auth.dto.ProfileUpdateRequest;

public interface ProfileService {

    ProfileResponse getCurrentProfile();

    ProfileResponse updateProfile(ProfileUpdateRequest request);

    void changePassword(ProfilePasswordRequest request);
}
