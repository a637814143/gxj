package com.gxj.cropyield.modules.weather.service.impl;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.gxj.cropyield.modules.weather.config.WeatherProperties;
import com.gxj.cropyield.modules.weather.service.WeatherService;
import com.gxj.cropyield.modules.weather.service.dto.WeatherRealtimeResponse;

/**
 * 天气服务实现，负责调用彩云天气接口并进行数据组装、缓存。
 */
@Service
public class WeatherServiceImpl implements WeatherService {

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherServiceImpl(@Qualifier("weatherRestTemplate") RestTemplate weatherRestTemplate, WeatherProperties properties) {
        this.restTemplate = weatherRestTemplate;
        this.properties = properties;
    }

    @Override
    public WeatherRealtimeResponse getRealtimeWeather(double longitude, double latitude) {
        String cacheKey = buildCacheKey(longitude, latitude);
        WeatherRealtimeResponse cached = getCachedWeather(cacheKey);
        if (cached != null) {
            return cached;
        }

        WeatherRealtimeResponse fresh = fetchRealtimeWeather(longitude, latitude);
        cache.put(cacheKey, new CachedWeather(fresh));
        return fresh;
    }

    private WeatherRealtimeResponse getCachedWeather(String cacheKey) {
        CachedWeather cachedWeather = cache.get(cacheKey);
        if (cachedWeather == null) {
            return null;
        }
        Duration ttl = Optional.ofNullable(properties.getCacheTtl()).orElse(Duration.ofMinutes(2));
        if (ttl.isNegative() || ttl.isZero()) {
            return null;
        }
        Instant now = Instant.now();
        if (cachedWeather.fetchedAt().plus(ttl).isAfter(now)) {
            return cachedWeather.payload();
        }
        cache.remove(cacheKey);
        return null;
    }

    private WeatherRealtimeResponse fetchRealtimeWeather(double longitude, double latitude) {
        WeatherProperties.CaiyunProperties caiyun = properties.getCaiyun();
        if (caiyun == null || !StringUtils.hasText(caiyun.getToken())) {
            throw new IllegalStateException("未配置彩云天气访问令牌");
        }
        String baseUrl = Optional.ofNullable(caiyun.getBaseUrl()).filter(StringUtils::hasText)
            .orElse("https://api.caiyunapp.com/v2.6");
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String location = longitude + "," + latitude;
        String url = trimmedBase + "/" + caiyun.getToken() + "/" + location + "/weather.json";

        URI requestUri = URI.create(url + "?alert=true&dailysteps=1&hourlysteps=1");
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(requestUri, JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("获取天气数据失败");
        }

        JsonNode body = response.getBody();
        String status = body.path("status").asText();
        if (!"ok".equalsIgnoreCase(status)) {
            throw new IllegalStateException("彩云天气返回异常状态: " + status);
        }

        JsonNode result = body.path("result");
        JsonNode realtime = result.path("realtime");
        JsonNode precipitation = realtime.path("precipitation");
        JsonNode wind = realtime.path("wind");
        JsonNode airQuality = realtime.path("air_quality");

        WeatherRealtimeResponse responseDto = new WeatherRealtimeResponse(
            longitude,
            latitude,
            status,
            realtime.path("skycon").asText(null),
            asDouble(realtime.path("temperature")),
            asDouble(realtime.path("apparent_temperature")),
            roundToSingleDecimal(asDouble(realtime.path("humidity")) * 100),
            asDouble(realtime.path("pressure")),
            asDouble(realtime.path("visibility")),
            new WeatherRealtimeResponse.Wind(
                asDouble(wind.path("speed")),
                asDouble(wind.path("direction"))
            ),
            new WeatherRealtimeResponse.Precipitation(
                asDouble(precipitation.path("local").path("intensity")),
                nullableDouble(precipitation.path("nearest").path("intensity")),
                nullableDouble(precipitation.path("nearest").path("distance"))
            ),
            buildAirQuality(airQuality),
            result.path("forecast_keypoint").asText(null),
            result.path("minutely").path("description").asText(null),
            parseInstant(realtime.path("obs_time").asText(null)),
            Instant.now()
        );

        return responseDto;
    }

    private WeatherRealtimeResponse.AirQuality buildAirQuality(JsonNode airQuality) {
        if (airQuality == null || airQuality.isMissingNode() || airQuality.isNull()) {
            return null;
        }
        JsonNode aqiNode = airQuality.path("aqi");
        JsonNode descriptionNode = airQuality.path("description");
        return new WeatherRealtimeResponse.AirQuality(
            nullableDouble(aqiNode.path("chn")),
            nullableDouble(airQuality.path("pm25")),
            nullableDouble(airQuality.path("pm10")),
            nullableDouble(airQuality.path("o3")),
            nullableDouble(airQuality.path("so2")),
            nullableDouble(airQuality.path("no2")),
            nullableDouble(airQuality.path("co")),
            descriptionNode.path("chn").asText(null),
            airQuality.path("description").path("color").asText(null)
        );
    }

    private double asDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Double.NaN;
        }
        return node.asDouble();
    }

    private Double nullableDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asDouble();
    }

    private double roundToSingleDecimal(double value) {
        if (Double.isNaN(value)) {
            return value;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String buildCacheKey(double longitude, double latitude) {
        return String.format("%.4f,%.4f", longitude, latitude);
    }

    private record CachedWeather(WeatherRealtimeResponse payload, Instant fetchedAt) {
        private CachedWeather {
            Objects.requireNonNull(payload, "payload");
            Objects.requireNonNull(fetchedAt, "fetchedAt");
        }

        private CachedWeather(WeatherRealtimeResponse payload) {
            this(payload, Instant.now());
        }
    }
}
