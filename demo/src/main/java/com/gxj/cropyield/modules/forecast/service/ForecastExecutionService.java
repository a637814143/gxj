package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionResponse;
/**
 * 预测管理模块的业务接口（接口），定义预测管理相关的核心业务操作。
 * <p>核心方法：runForecast。</p>
 */

public interface ForecastExecutionService {

    ForecastExecutionResponse runForecast(ForecastExecutionRequest request);
}
