package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.ProfilePasswordRequest;
import com.gxj.cropyield.modules.auth.dto.ProfileResponse;
import com.gxj.cropyield.modules.auth.dto.ProfileUpdateRequest;
/**
 * 认证与账号模块的业务接口（接口），定义认证与账号相关的核心业务操作。
 * <p>核心方法：getCurrentProfile、updateProfile、changePassword。</p>
 */

public interface ProfileService {

    ProfileResponse getCurrentProfile();

    ProfileResponse updateProfile(ProfileUpdateRequest request);

    void changePassword(ProfilePasswordRequest request);

    void sendPasswordChangeCode(String ipAddress);
}
