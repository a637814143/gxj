package com.dali.cropyield.modules.auth.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.auth.dto.UserRequest;
import com.dali.cropyield.modules.auth.dto.UserResponse;
import com.dali.cropyield.modules.auth.entity.User;
import com.dali.cropyield.modules.auth.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        List<UserResponse> responses = userService.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setEnabled(Boolean.TRUE);
        user.setLocked(Boolean.FALSE);
        return ApiResponse.success(UserResponse.from(userService.save(user)));
    }
}
