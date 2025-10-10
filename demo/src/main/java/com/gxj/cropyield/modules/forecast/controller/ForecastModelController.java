package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecast/models")
public class ForecastModelController {

    private final ForecastModelService forecastModelService;

    public ForecastModelController(ForecastModelService forecastModelService) {
        this.forecastModelService = forecastModelService;
    }

    @GetMapping
    public ApiResponse<List<ForecastModel>> listModels() {
        return ApiResponse.success(forecastModelService.listAll());
    }

    @PostMapping
    public ApiResponse<ForecastModel> createModel(@Valid @RequestBody ForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.create(request));
    }
}
