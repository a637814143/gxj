package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryResponse;
import com.gxj.cropyield.modules.forecast.service.ForecastHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
public class ForecastHistoryController {

    private final ForecastHistoryService forecastHistoryService;

    public ForecastHistoryController(ForecastHistoryService forecastHistoryService) {
        this.forecastHistoryService = forecastHistoryService;
    }

    @GetMapping("/history")
    public ApiResponse<List<ForecastHistoryResponse>> history(@RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(forecastHistoryService.getRecentHistory(limit));
    }
}
