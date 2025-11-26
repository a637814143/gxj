package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.config.ForecastModelSettings;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelOptionsResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.dto.ToggleForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预测管理模块的控制器，用于暴露预测管理相关的 REST 接口。
 * <p>核心方法：listModels、createModel、updateModel、duplicateModel、toggleEnabled。</p>
 */

@RestController
@RequestMapping("/api/forecast/models")
public class ForecastModelController {

    private final ForecastModelService forecastModelService;
    private final ForecastModelSettings forecastModelSettings;

    public ForecastModelController(ForecastModelService forecastModelService,
                                   ForecastModelSettings forecastModelSettings) {
        this.forecastModelService = forecastModelService;
        this.forecastModelSettings = forecastModelSettings;
    }

    @GetMapping
    public ApiResponse<List<ForecastModel>> listModels() {
        return ApiResponse.success(forecastModelService.listAll());
    }

    @GetMapping("/available")
    public ApiResponse<List<ForecastModel>> listAvailableModels() {
        return ApiResponse.success(forecastModelService.listAvailable());
    }

    @GetMapping("/options")
    public ApiResponse<ForecastModelOptionsResponse> listAllowedTypes() {
        return ApiResponse.success(new ForecastModelOptionsResponse(forecastModelSettings.getAllowedTypes()));
    }

    @PostMapping
    public ApiResponse<ForecastModel> createModel(@Valid @RequestBody ForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ForecastModel> updateModel(@PathVariable Long id, @Valid @RequestBody ForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.update(id, request));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<ForecastModel> duplicateModel(@PathVariable Long id) {
        return ApiResponse.success(forecastModelService.duplicate(id));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<ForecastModel> toggleModel(@PathVariable Long id, @Valid @RequestBody ToggleForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.toggleEnabled(id, request.enabled()));
    }
}
