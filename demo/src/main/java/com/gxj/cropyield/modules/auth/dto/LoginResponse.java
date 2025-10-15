package com.gxj.cropyield.modules.auth.dto;

public record LoginResponse(
    AuthTokens tokens,
    UserInfo user
) {
}
