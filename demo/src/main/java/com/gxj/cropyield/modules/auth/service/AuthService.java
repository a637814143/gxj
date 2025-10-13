package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
