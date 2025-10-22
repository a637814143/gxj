package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.UserInfo;
/**
 * 认证与账号模块的业务接口（接口），定义认证与账号相关的核心业务操作。
 * <p>核心方法：login、loginAsAdmin、loginAsUser、register、refreshToken、getCurrentUser、hasRole。</p>
 */

public interface AuthService {

    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    LoginResponse loginAsAdmin(LoginRequest request, String ipAddress, String userAgent);

    LoginResponse loginAsUser(LoginRequest request, String ipAddress, String userAgent);

    LoginResponse register(RegisterRequest request, String ipAddress, String userAgent);

    AuthTokens refreshToken(String refreshToken);

    UserInfo getCurrentUser();

    boolean hasRole(String roleCode);
}
