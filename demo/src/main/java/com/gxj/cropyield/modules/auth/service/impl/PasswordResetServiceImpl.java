package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.PasswordResetCodeRequest;
import com.gxj.cropyield.modules.auth.dto.PasswordResetRequest;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.EmailVerificationService;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import com.gxj.cropyield.modules.auth.service.PasswordPolicyValidator;
import com.gxj.cropyield.modules.auth.service.PasswordResetService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证与账号模块的业务实现类，完成找回密码流程的具体逻辑。
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    EmailVerificationService emailVerificationService,
                                    PasswordPolicyValidator passwordPolicyValidator,
                                    PasswordEncoder passwordEncoder,
                                    LoginLogService loginLogService) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.passwordEncoder = passwordEncoder;
        this.loginLogService = loginLogService;
    }

    @Override
    @Transactional
    public void sendResetCode(PasswordResetCodeRequest request, String ipAddress) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱不正确"));
        if (!isEmailMatched(user, request.email())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱不正确");
        }
        emailVerificationService.sendVerificationCode(request.email(), request.captchaId(), request.captchaCode(), ipAddress);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱不正确"));
        if (!isEmailMatched(user, request.email())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱不正确");
        }
        passwordPolicyValidator.validate(request.newPassword());
        emailVerificationService.validateAndConsume(request.email(), request.emailCode());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        loginLogService.record(user.getUsername(), true, ipAddress, userAgent, "密码重置成功");
    }

    private boolean isEmailMatched(User user, String email) {
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(email)) {
            return false;
        }
        return user.getEmail().trim().equalsIgnoreCase(email.trim());
    }
}
