package com.gxj.cropyield.modules.auth.service;

public interface LoginLogService {

    void record(String username, boolean success, String ipAddress, String userAgent, String message);
}
