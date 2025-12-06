package com.gxj.cropyield.common.logging;

import com.gxj.cropyield.modules.system.service.SystemLogService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
/**
 * 审计日志模块的日志组件，记录审计日志的操作轨迹。
 */

@Component
public class AuditLogger {

    private final SystemLogService systemLogService;

    public AuditLogger(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    public void record(String action, String detail) {
        systemLogService.record(resolveUsername(), action, detail);
    }

    public void recordForUser(String username, String action, String detail) {
        systemLogService.record(username, action, detail);
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String str) {
            return str;
        }
        return authentication.getName();
    }
}
