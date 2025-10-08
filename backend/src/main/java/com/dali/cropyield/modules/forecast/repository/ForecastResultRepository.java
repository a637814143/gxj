package com.dali.cropyield.modules.forecast.repository;

import com.dali.cropyield.modules.forecast.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {
}
