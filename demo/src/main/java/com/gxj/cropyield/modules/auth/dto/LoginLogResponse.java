package com.gxj.cropyield.modules.auth.dto;

import com.gxj.cropyield.modules.auth.entity.LoginLog;

import java.time.LocalDateTime;

public record LoginLogResponse(
    Long id,
    String username,
    boolean success,
    String ipAddress,
    String userAgent,
    String message,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static LoginLogResponse from(LoginLog entity) {
        if (entity == null) {
            return null;
        }
        return new LoginLogResponse(
            entity.getId(),
            entity.getUsername(),
            entity.isSuccess(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getMessage(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
