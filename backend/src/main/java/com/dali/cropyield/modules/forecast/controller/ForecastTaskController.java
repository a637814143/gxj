package com.dali.cropyield.modules.forecast.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastTask;
import com.dali.cropyield.modules.forecast.service.ForecastTaskService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast/tasks")
@RequiredArgsConstructor
public class ForecastTaskController {

    private final ForecastTaskService forecastTaskService;

    @GetMapping
    public ApiResponse<List<ForecastTask>> list() {
        return ApiResponse.success(forecastTaskService.findAll());
    }

    @PostMapping
    public ApiResponse<ForecastTask> create(@Valid @RequestBody ForecastTaskRequest request) {
        return ApiResponse.success(forecastTaskService.create(request));
    }
}
