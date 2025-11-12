package com.gxj.cropyield.modules.dataset.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.WeatherDatasetResponse;
import com.gxj.cropyield.modules.dataset.dto.WeatherRegionOption;
import com.gxj.cropyield.modules.dataset.service.WeatherRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 气象数据接口。
 */
@RestController
@RequestMapping("/api/datasets/weather-records")
public class WeatherRecordController {

    private final WeatherRecordService weatherRecordService;

    public WeatherRecordController(WeatherRecordService weatherRecordService) {
        this.weatherRecordService = weatherRecordService;
    }

    @GetMapping
    public ApiResponse<WeatherDatasetResponse> listRecords(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit
    ) {
        WeatherDatasetResponse payload = weatherRecordService.queryDataset(regionId, startDate, endDate, limit);
        return ApiResponse.success(payload);
    }

    @GetMapping("/regions")
    public ApiResponse<List<WeatherRegionOption>> listRegionOptions() {
        return ApiResponse.success(weatherRecordService.listRegionsWithRecords());
    }
}
