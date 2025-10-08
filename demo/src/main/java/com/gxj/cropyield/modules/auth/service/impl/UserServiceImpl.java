package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RoleRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(user -> new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
            ));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        userRepository.findByUsername(request.username())
            .ifPresent(user -> {
                throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
            });

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setFullName(request.fullName());
        user.setEmail(request.email());

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllById(request.roleIds()).stream().collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        return new UserResponse(
            saved.getId(),
            saved.getUsername(),
            saved.getFullName(),
            saved.getEmail(),
            saved.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
            saved.getCreatedAt()
        );
    }
}
