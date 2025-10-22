package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    Optional<ForecastResult> findByTaskIdAndTargetYear(Long taskId, Integer targetYear);
}
