package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.AuthTokens;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.dto.UserInfo;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    LoginResponse register(RegisterRequest request, String ipAddress, String userAgent);

    AuthTokens refreshToken(String refreshToken);

    UserInfo getCurrentUser();

    boolean hasRole(String roleCode);
}
