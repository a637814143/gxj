package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
    Optional<ForecastModel> findByName(String name);
}
