package com.gxj.cropyield.modules.auth.dto;

import com.gxj.cropyield.modules.auth.entity.LoginLog;

import java.time.LocalDateTime;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

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
