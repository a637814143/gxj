package com.gxj.cropyield.modules.weather.controller;

import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.weather.service.WeatherService;
import com.gxj.cropyield.modules.weather.service.dto.WeatherRealtimeResponse;

/**
 * 天气接口控制器，为前端提供实时天气查询能力。
 */
@RestController
@RequestMapping("/api/weather")
@Validated
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/realtime")
    public ApiResponse<WeatherRealtimeResponse> fetchRealtimeWeather(
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) String location
    ) {
        double resolvedLongitude;
        double resolvedLatitude;

        if (StringUtils.hasText(location)) {
            String[] parts = location.split(",");
            if (parts.length != 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "location 参数格式错误，应为 '经度,纬度'");
            }
            try {
                resolvedLongitude = Double.parseDouble(parts[0].trim());
                resolvedLatitude = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException ex) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "location 参数包含非法数字");
            }
        } else {
            if (longitude == null || latitude == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "缺少经纬度参数");
            }
            resolvedLongitude = longitude;
            resolvedLatitude = latitude;
        }

        WeatherRealtimeResponse payload = weatherService.getRealtimeWeather(resolvedLongitude, resolvedLatitude);
        return ApiResponse.success(payload);
    }
}
