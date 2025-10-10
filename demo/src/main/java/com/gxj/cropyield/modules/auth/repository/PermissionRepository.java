package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
