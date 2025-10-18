package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.RoleSummary;
import com.gxj.cropyield.modules.auth.dto.UserPasswordRequest;
import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import com.gxj.cropyield.modules.auth.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    Page<UserResponse> listUsers(Pageable pageable);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    void updatePassword(Long userId, UserPasswordRequest request);

    List<RoleSummary> listRoles();
}
