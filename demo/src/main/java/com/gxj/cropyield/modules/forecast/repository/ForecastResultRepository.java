package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    @EntityGraph(attributePaths = {"task", "task.model", "task.crop", "task.region"})
    Optional<ForecastResult> findByTaskIdAndTargetYear(Long taskId, Integer targetYear);
    
    // 批量查询优化
    @EntityGraph(attributePaths = {"task", "task.model", "task.crop", "task.region"})
    List<ForecastResult> findByTaskIdIn(List<Long> taskIds);
}
