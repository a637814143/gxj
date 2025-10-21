package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {

    long countBySuccess(boolean success);

    Optional<LoginLog> findFirstBySuccessOrderByCreatedAtDesc(boolean success);
}
