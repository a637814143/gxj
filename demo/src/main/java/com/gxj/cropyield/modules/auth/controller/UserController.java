package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.PageResponse;
import com.gxj.cropyield.modules.auth.dto.RoleSummary;
import com.gxj.cropyield.modules.auth.dto.UserPasswordRequest;
import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import com.gxj.cropyield.modules.auth.dto.UserUpdateRequest;
import com.gxj.cropyield.modules.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserResponse> result = userService.listUsers(PageRequest.of(page, size));
        return ApiResponse.success(new PageResponse<>(result.getContent(), result.getTotalElements(), page, size));
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
        @PathVariable("id") Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> updatePassword(
        @PathVariable("id") Long id,
        @Valid @RequestBody UserPasswordRequest request
    ) {
        userService.updatePassword(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleSummary>> listRoles() {
        return ApiResponse.success(userService.listRoles());
    }
}
