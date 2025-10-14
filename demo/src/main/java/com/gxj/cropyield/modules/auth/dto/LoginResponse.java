package com.gxj.cropyield.modules.auth.dto;

import java.util.Set;

public record LoginResponse(
    Long userId,
    String username,
    String fullName,
    String token,
    Set<String> roles
) {
}
