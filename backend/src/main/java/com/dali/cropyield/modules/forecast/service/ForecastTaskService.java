package com.dali.cropyield.modules.forecast.service;

import com.dali.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastTask;
import java.util.List;

public interface ForecastTaskService {

    List<ForecastTask> findAll();

    ForecastTask create(ForecastTaskRequest request);
}
