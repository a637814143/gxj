package com.gxj.cropyield.modules.system.repository;

import com.gxj.cropyield.modules.system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findTopByOrderByIdAsc();
}
