package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionResponse;

public interface ForecastExecutionService {

    ForecastExecutionResponse runForecast(ForecastExecutionRequest request);
}
