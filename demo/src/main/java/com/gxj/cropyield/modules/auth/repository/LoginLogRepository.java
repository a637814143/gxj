package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
