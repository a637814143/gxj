package com.gxj.cropyield.modules.weather.service;

import com.gxj.cropyield.modules.weather.service.dto.WeatherForecastResponse;
import com.gxj.cropyield.modules.weather.service.dto.WeatherRealtimeResponse;

/**
 * 天气服务接口，定义天气数据获取相关的功能。
 */
public interface WeatherService {

    /**
     * 获取指定经纬度的实时天气数据。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 实时天气响应数据
     */
    WeatherRealtimeResponse getRealtimeWeather(double longitude, double latitude);

    /**
     * 获取指定经纬度的逐日天气预报。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 未来天气预报数据
     */
    WeatherForecastResponse getDailyForecast(double longitude, double latitude);
}
