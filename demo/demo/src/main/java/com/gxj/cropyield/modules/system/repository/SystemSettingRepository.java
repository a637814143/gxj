package com.gxj.cropyield.modules.system.repository;

import com.gxj.cropyield.modules.system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * 系统设置模块的数据访问接口（接口），封装了对系统设置相关数据表的持久化操作。
 */

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findTopByOrderByIdAsc();
}
