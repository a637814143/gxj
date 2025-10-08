package com.dali.cropyield.modules.forecast.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastModel;
import com.dali.cropyield.modules.forecast.service.ForecastModelService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast/models")
@RequiredArgsConstructor
public class ForecastModelController {

    private final ForecastModelService forecastModelService;

    @GetMapping
    public ApiResponse<List<ForecastModel>> list() {
        return ApiResponse.success(forecastModelService.findAll());
    }

    @PostMapping
    public ApiResponse<ForecastModel> create(@Valid @RequestBody ForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.create(request));
    }
}
