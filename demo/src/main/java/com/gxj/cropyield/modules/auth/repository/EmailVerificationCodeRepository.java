package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
/**
 * 认证与账号模块的数据访问接口，提供邮箱验证码的持久化能力。
 */

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerificationCode> findTopByEmailAndCodeOrderByCreatedAtDesc(String email, String code);

    long countByRequestedIpAndCreatedAtAfter(String requestedIp, LocalDateTime threshold);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
