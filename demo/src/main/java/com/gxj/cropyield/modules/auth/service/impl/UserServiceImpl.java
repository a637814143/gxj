package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.audit.AuditLog;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.RoleSummary;
import com.gxj.cropyield.modules.auth.dto.UserPasswordRequest;
import com.gxj.cropyield.modules.auth.dto.UserRequest;
import com.gxj.cropyield.modules.auth.dto.UserResponse;
import com.gxj.cropyield.modules.auth.dto.UserUpdateRequest;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RoleRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.UserService;
import com.gxj.cropyield.modules.notification.dto.EmailNotificationRequest;
import com.gxj.cropyield.modules.notification.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 认证与账号模块的业务实现类，负责落实认证与账号领域的业务处理逻辑。
 * <p>核心方法：listUsers、createUser、toResponse、updateUser、deleteUser、updatePassword、listRoles、resolveRoles。</p>
 */

/**
 * 用户管理服务实现类 - 用户CRUD、角色分配、密码重置
 */
@Service
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional
    @AuditLog(operation = "CREATE_USER", module = "用户管理", description = "创建用户")
    public UserResponse createUser(UserRequest request) {
        log.info("开始创建用户 - 用户名: {}, 邮箱: {}", request.username(), request.email());
        
        try {
            userRepository.findByUsername(request.username())
                .ifPresent(user -> {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
                });

            User user = new User();
            user.setUsername(request.username());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setFullName(request.fullName());
            user.setEmail(request.email());
            user.setRoles(resolveRoles(request.roleIds()));

            User saved = userRepository.save(user);
            log.info("用户创建成功 - ID: {}, 用户名: {}", saved.getId(), saved.getUsername());
            return toResponse(saved);
            
        } catch (Exception e) {
            log.error("用户创建失败 - 用户名: {}, 错误: {}", request.username(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @AuditLog(operation = "UPDATE_USER", module = "用户管理", description = "更新用户信息")
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.info("开始更新用户 - ID: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));

            String oldFullName = user.getFullName();
            String oldEmail = user.getEmail();
            
            if (request.fullName() != null) {
                user.setFullName(request.fullName());
            }
            if (request.email() != null) {
                user.setEmail(request.email());
            }
            if (request.roleIds() != null) {
                user.setRoles(resolveRoles(request.roleIds()));
            }

            User saved = userRepository.save(user);
            log.info("用户更新成功 - ID: {}, 用户名: {}, 变更: 姓名[{}->{}], 邮箱[{}->{}]", 
                    saved.getId(), saved.getUsername(), oldFullName, saved.getFullName(), 
                    oldEmail, saved.getEmail());
            return toResponse(saved);
            
        } catch (Exception e) {
            log.error("用户更新失败 - ID: {}, 错误: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @AuditLog(operation = "DELETE_USER", module = "用户管理", description = "删除用户")
    public void deleteUser(Long userId) {
        log.info("开始删除用户 - ID: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
            
            String username = user.getUsername();
            userRepository.delete(user);
            log.info("用户删除成功 - ID: {}, 用户名: {}", userId, username);
            
        } catch (Exception e) {
            log.error("用户删除失败 - ID: {}, 错误: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @AuditLog(operation = "RESET_PASSWORD", module = "用户管理", description = "重置用户密码", recordParams = false)
    public void updatePassword(Long userId, UserPasswordRequest request) {
        log.info("开始重置用户密码 - ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        
        String defaultPassword = user.getUsername() + "123456";
        user.setPassword(passwordEncoder.encode(defaultPassword));
        userRepository.save(user);

        String email = user.getEmail();
        if (!StringUtils.hasText(email)) {
            log.warn("用户未绑定邮箱，无法发送重置密码通知 - ID: {}, 用户名: {}", userId, user.getUsername());
        } else {
            emailNotificationService.sendEmailNotification(new EmailNotificationRequest(
                email.trim(),
                "密码已重置通知",
                "PASSWORD_RESET",
                Map.of(
                    "username", user.getUsername(),
                    "newPassword", defaultPassword
                )
            ));
        }
        
        log.info("用户密码重置成功 - ID: {}, 用户名: {}", userId, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummary> listRoles() {
        return roleRepository.findAll().stream()
            .map(role -> new RoleSummary(role.getId(), role.getCode(), role.getName()))
            .collect(Collectors.toList());
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在无效的角色标识");
        }
        return new HashSet<>(roles);
    }

    private UserResponse toResponse(User user) {
        Set<RoleSummary> roleSummaries = user.getRoles().stream()
            .map(role -> new RoleSummary(role.getId(), role.getCode(), role.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            roleSummaries,
            user.getCreatedAt()
        );
    }
}
