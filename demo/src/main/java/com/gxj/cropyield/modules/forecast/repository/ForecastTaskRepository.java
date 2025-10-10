package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastTaskRepository extends JpaRepository<ForecastTask, Long> {
}
