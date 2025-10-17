package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    List<ForecastResult> findByTaskId(Long taskId);
}
