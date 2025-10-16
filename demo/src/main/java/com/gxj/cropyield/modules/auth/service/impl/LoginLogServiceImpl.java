package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.modules.auth.entity.LoginLog;
import com.gxj.cropyield.modules.auth.repository.LoginLogRepository;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogServiceImpl(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @Override
    public void record(String username, boolean success, String ipAddress, String userAgent, String message) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setSuccess(success);
        log.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "UNKNOWN" : ipAddress);
        log.setUserAgent(userAgent == null || userAgent.isBlank() ? "UNKNOWN" : userAgent);
        log.setMessage(message);
        loginLogRepository.save(log);
    }
}
