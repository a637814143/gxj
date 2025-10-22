package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ProfileResponse(
    Long id,
    String username,
    String fullName,
    String email,
    Set<RoleSummary> roles,
    LocalDateTime createdAt
) {
}
