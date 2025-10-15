package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastRunSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForecastRunSeriesRepository extends JpaRepository<ForecastRunSeries, Long> {

    List<ForecastRunSeries> findByRunIdOrderByPeriodAsc(Long runId);
}
