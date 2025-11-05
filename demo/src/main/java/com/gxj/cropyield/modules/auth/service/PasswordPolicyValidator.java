package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 认证与账号模块的领域服务，负责校验密码是否符合安全策略要求。
 */
@Component
public class PasswordPolicyValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern DIGIT_ONLY_PATTERN = Pattern.compile("\\d+");

    public void validate(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        String password = rawPassword.trim();
        if (!UPPERCASE_PATTERN.matcher(password).find()
                || !LOWERCASE_PATTERN.matcher(password).find()
                || !DIGIT_PATTERN.matcher(password).find()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码需同时包含大写字母、小写字母和数字");
        }
        if (isSequentialDigits(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为连续数字");
        }
    }

    private boolean isSequentialDigits(String password) {
        if (!DIGIT_ONLY_PATTERN.matcher(password).matches() || password.length() < 3) {
            return false;
        }
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < password.length(); i++) {
            int prev = password.charAt(i - 1) - '0';
            int current = password.charAt(i) - '0';
            if (current - prev != 1) {
                ascending = false;
            }
            if (current - prev != -1) {
                descending = false;
            }
            if (!ascending && !descending) {
                return false;
            }
        }
        return ascending || descending;
    }
}
