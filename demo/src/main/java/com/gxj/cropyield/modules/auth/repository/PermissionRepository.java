package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 认证与账号模块的数据访问接口（接口），封装了对认证与账号相关数据表的持久化操作。
 */

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
