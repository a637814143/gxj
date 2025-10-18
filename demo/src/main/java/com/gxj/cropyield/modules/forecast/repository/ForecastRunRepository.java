package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastRunRepository extends JpaRepository<ForecastRun, Long> {

    boolean existsByCropId(Long cropId);

    boolean existsByRegionId(Long regionId);
}
