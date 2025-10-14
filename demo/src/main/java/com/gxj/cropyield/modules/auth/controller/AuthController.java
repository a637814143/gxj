package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.auth.dto.LoginRequest;
import com.gxj.cropyield.modules.auth.dto.LoginResponse;
import com.gxj.cropyield.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
