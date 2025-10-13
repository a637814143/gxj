package com.gxj.cropyield.user.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserProfileResponse(
        String username,
        String displayName,
        String email,
        String phone,
        String organization,
        Set<String> roles,
        LocalDateTime lastLoginAt
) {
}
