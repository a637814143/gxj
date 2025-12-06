package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastRunRepository extends JpaRepository<ForecastRun, Long> {

    boolean existsByCropId(Long cropId);

    boolean existsByRegionId(Long regionId);
}
