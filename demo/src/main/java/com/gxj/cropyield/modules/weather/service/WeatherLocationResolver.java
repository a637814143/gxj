package com.gxj.cropyield.modules.weather.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.weather.config.WeatherProperties;

/**
 * 天气服务的辅助组件，负责将区域解析为经纬度，供和风天气等外部预报接口调用。
 */
@Component
public class WeatherLocationResolver {

    private static final Logger log = LoggerFactory.getLogger(WeatherLocationResolver.class);

    private final WeatherProperties properties;

    public WeatherLocationResolver(WeatherProperties properties) {
        this.properties = properties;
    }

    public Optional<Coordinate> resolve(Region region) {
        if (region == null) {
            return Optional.empty();
        }
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather != null) {
            String code = region.getCode();
            if (StringUtils.hasText(code)) {
                Optional<Coordinate> mapped = parseCoordinate(qweather.getRegionLocations().get(code));
                if (mapped.isPresent()) {
                    return mapped;
                }
            }
            Optional<Coordinate> defaultLocation = parseCoordinate(qweather.getDefaultLocation());
            if (defaultLocation.isPresent()) {
                return defaultLocation;
            }
        }
        Optional<Coordinate> descriptionLocation = parseCoordinate(region.getDescription());
        if (descriptionLocation.isPresent()) {
            return descriptionLocation;
        }
        log.debug("未找到区域 {} 的经纬度映射", region.getCode());
        return Optional.empty();
    }

    private Optional<Coordinate> parseCoordinate(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            double longitude = Double.parseDouble(parts[0].trim());
            double latitude = Double.parseDouble(parts[1].trim());
            return Optional.of(new Coordinate(longitude, latitude));
        } catch (NumberFormatException ex) {
            log.debug("经纬度格式解析失败: {}", value, ex);
            return Optional.empty();
        }
    }

    public record Coordinate(double longitude, double latitude) {
    }
}
