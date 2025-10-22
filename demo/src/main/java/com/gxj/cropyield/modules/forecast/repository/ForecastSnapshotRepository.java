package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, Long> {

    @EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
    List<ForecastSnapshot> findByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
    List<ForecastSnapshot> findByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
    Page<ForecastSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
    List<ForecastSnapshot> findByRunIdOrderByPeriodAsc(Long runId);
}
