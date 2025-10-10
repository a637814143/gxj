package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
}
