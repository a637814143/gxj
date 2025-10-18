package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String username,
    String fullName,
    String email,
    Set<RoleSummary> roles,
    LocalDateTime createdAt
) {
}
