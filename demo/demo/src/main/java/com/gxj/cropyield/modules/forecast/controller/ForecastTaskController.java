package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 预测管理模块的控制器，用于暴露预测管理相关的 REST 接口。
 * <p>核心方法：listTasks、createTask。</p>
 */

@RestController
@RequestMapping("/api/forecast/tasks")
public class ForecastTaskController {

    private final ForecastTaskService forecastTaskService;

    public ForecastTaskController(ForecastTaskService forecastTaskService) {
        this.forecastTaskService = forecastTaskService;
    }

    @GetMapping
    public ApiResponse<List<ForecastTask>> listTasks() {
        return ApiResponse.success(forecastTaskService.listAll());
    }

    @PostMapping
    public ApiResponse<ForecastTask> createTask(@Valid @RequestBody ForecastTaskRequest request) {
        return ApiResponse.success(forecastTaskService.create(request));
    }
}
