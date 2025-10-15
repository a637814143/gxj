package com.gxj.cropyield.modules.auth.dto;

import java.util.Set;

public record UserInfo(
    Long id,
    String username,
    String fullName,
    String email,
    Set<String> roles
) {
}
