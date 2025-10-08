package com.dali.cropyield.modules.auth.dto;

import com.dali.cropyield.modules.auth.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        Boolean enabled,
        Boolean locked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getEnabled(),
                user.getLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
