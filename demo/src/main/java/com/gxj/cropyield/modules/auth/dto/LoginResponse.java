package com.gxj.cropyield.modules.auth.dto;

import java.util.Set;

public record LoginResponse(
    String token,
    String username,
    Set<String> roles
) {
}
