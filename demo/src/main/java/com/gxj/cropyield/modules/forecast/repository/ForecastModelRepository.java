package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
    Optional<ForecastModel> findByName(String name);

    Optional<ForecastModel> findByType(ForecastModel.ModelType type);
}
