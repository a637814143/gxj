package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> listUsers(Pageable pageable);

    UserResponse createUser(UserRequest request);
}
