package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
/**
 * 认证与账号模块的数据访问接口（接口），封装了对认证与账号相关数据表的持久化操作。
 */

public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {

    long countBySuccess(boolean success);

    Optional<LoginLog> findFirstBySuccessOrderByCreatedAtDesc(boolean success);
}
