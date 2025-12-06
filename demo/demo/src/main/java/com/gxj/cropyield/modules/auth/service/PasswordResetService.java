package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.PasswordResetCodeRequest;
import com.gxj.cropyield.modules.auth.dto.PasswordResetRequest;

/**
 * 认证与账号模块的业务接口，定义找回密码流程相关的核心操作。
 */
public interface PasswordResetService {

    void sendResetCode(PasswordResetCodeRequest request, String ipAddress);

    void resetPassword(PasswordResetRequest request, String ipAddress, String userAgent);
}
