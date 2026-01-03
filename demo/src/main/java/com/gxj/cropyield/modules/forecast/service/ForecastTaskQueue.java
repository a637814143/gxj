package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;

public interface ForecastTaskQueue {

    void publish(ForecastExecutionRequest request);

    ForecastExecutionRequest poll();
}
