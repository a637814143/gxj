package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    Optional<ForecastResult> findByTaskIdAndTargetYear(Long taskId, Integer targetYear);
}
