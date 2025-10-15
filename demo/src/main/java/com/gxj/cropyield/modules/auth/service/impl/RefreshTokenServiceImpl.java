package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.config.JwtProperties;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.RefreshToken;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RefreshTokenRepository;
import com.gxj.cropyield.modules.auth.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public RefreshToken create(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(generateTokenValue());
        token.setExpiresAt(calculateExpiry());
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效"));
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌已过期");
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshToken rotate(RefreshToken token) {
        token.setToken(generateTokenValue());
        token.setExpiresAt(calculateExpiry());
        return refreshTokenRepository.save(token);
    }

    private LocalDateTime calculateExpiry() {
        long refreshExpiration = jwtProperties.getRefreshExpiration();
        return LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000);
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
