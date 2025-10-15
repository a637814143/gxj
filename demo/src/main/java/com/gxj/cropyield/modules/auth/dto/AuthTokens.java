package com.gxj.cropyield.modules.auth.dto;

public record AuthTokens(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshToken
) {
}
