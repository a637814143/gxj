package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;

import java.util.List;

public interface ForecastTaskService {

    List<ForecastTask> listAll();

    ForecastTask create(ForecastTaskRequest request);
}
