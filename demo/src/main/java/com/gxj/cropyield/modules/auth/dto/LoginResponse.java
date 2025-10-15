package com.gxj.cropyield.modules.auth.dto;

import java.util.Set;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresIn,
    Long id,
    String username,
    String fullName,
    String email,
    Set<String> roles
) {
}
