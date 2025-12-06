package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastTaskRepository extends JpaRepository<ForecastTask, Long> {

    boolean existsByCropId(Long cropId);

    boolean existsByRegionId(Long regionId);

    Optional<ForecastTask> findByModelIdAndCropIdAndRegionId(Long modelId, Long cropId, Long regionId);
}
