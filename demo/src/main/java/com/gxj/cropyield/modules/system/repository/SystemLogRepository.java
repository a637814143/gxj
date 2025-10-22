package com.gxj.cropyield.modules.system.repository;

import com.gxj.cropyield.modules.system.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 系统设置模块的数据访问接口（接口），封装了对系统设置相关数据表的持久化操作。
 */

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
}
