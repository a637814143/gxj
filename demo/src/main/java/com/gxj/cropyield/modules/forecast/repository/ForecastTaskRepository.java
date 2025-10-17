package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForecastTaskRepository extends JpaRepository<ForecastTask, Long> {

    Optional<ForecastTask> findByModelIdAndCropIdAndRegionId(Long modelId, Long cropId, Long regionId);
}
