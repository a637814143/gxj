package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryPageResponse;
import com.gxj.cropyield.modules.forecast.service.ForecastHistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastHistoryController {

    private final ForecastHistoryService forecastHistoryService;

    public ForecastHistoryController(ForecastHistoryService forecastHistoryService) {
        this.forecastHistoryService = forecastHistoryService;
    }

    @GetMapping("/history")
    public ApiResponse<ForecastHistoryPageResponse> history(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "5") int size
    ) {
        return ApiResponse.success(forecastHistoryService.getHistory(page, size));
    }

    @DeleteMapping("/history/{runId}")
    public ApiResponse<Void> deleteHistory(@PathVariable Long runId) {
        forecastHistoryService.deleteHistory(runId);
        return ApiResponse.success();
    }
}
