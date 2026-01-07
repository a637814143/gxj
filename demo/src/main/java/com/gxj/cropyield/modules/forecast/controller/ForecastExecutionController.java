package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionResponse;
import com.gxj.cropyield.modules.forecast.service.ForecastExecutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 预测管理模块的控制器，用于暴露预测管理相关的 REST 接口。
 * <p>核心方法：predict。</p>
 */

/**
 * 产量预测执行控制器 - 执行预测任务、获取预测结果
 */
@RestController
@RequestMapping("/api/forecast")
public class ForecastExecutionController {

    private final ForecastExecutionService forecastExecutionService;

    public ForecastExecutionController(ForecastExecutionService forecastExecutionService) {
        this.forecastExecutionService = forecastExecutionService;
    }

    @PostMapping("/predict")
    public ApiResponse<ForecastExecutionResponse> predict(@Valid @RequestBody ForecastExecutionRequest request) {
        return ApiResponse.success(forecastExecutionService.runForecast(request));
    }
}
