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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.weather.config.WeatherProperties;
import com.gxj.cropyield.modules.weather.config.WeatherProperties.QWeatherProperties;
import com.gxj.cropyield.modules.weather.config.WeatherProperties.QWeatherProperties.LocationMode;
import com.gxj.cropyield.modules.weather.service.QWeatherLocationClient;
import com.gxj.cropyield.modules.weather.service.WeatherService;
import com.gxj.cropyield.modules.weather.service.dto.WeatherRealtimeResponse;

/**
 * 天气服务实现，负责调用和风天气接口并进行数据组装、缓存。
 */
@Service
public class WeatherServiceImpl implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;
    private final QWeatherLocationClient locationClient;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherServiceImpl(@Qualifier("weatherRestTemplate") RestTemplate weatherRestTemplate,
                              WeatherProperties properties,
                              QWeatherLocationClient locationClient) {
        this.restTemplate = weatherRestTemplate;
        this.properties = properties;
        this.locationClient = locationClient;
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
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather == null || !StringUtils.hasText(qweather.getKey())) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "未配置和风天气访问密钥");
        }
        String baseUrl = Optional.ofNullable(qweather.getBaseUrl()).filter(StringUtils::hasText)
            .orElse("https://m776x8rde7.re.qweatherapi.com/v7");
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/weather/now")
            .queryParam("location", resolveLocationParameter(longitude, latitude, qweather))
            .queryParam("lang", qweather.getLanguage())
            .queryParam("unit", qweather.getUnit());
        if (shouldAppendQueryKey(qweather)) {
            uriBuilder.queryParam("key", qweather.getKey());
        }
        URI requestUri = uriBuilder.build(true).toUri();

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.getForEntity(requestUri, JsonNode.class);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException("实时", ex);
        } catch (RestClientException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "调用和风天气实时接口失败: " + ex.getMessage());
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "获取天气数据失败");
        }

        JsonNode body = response.getBody();
        String status = body.path("code").asText();
        if (!"200".equals(status)) {
            String errorMessage = body.path("message").asText();
            if (!StringUtils.hasText(errorMessage)) {
                errorMessage = body.path("refer").path("license").toString();
            }
            if (StringUtils.hasText(errorMessage)) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "和风天气返回异常状态: " + errorMessage);
            }
            throw new BusinessException(ResultCode.SERVER_ERROR, "和风天气返回错误码: " + status);
        }

        JsonNode nowNode = body.path("now");
        WeatherRealtimeResponse.AirQuality airQuality = fetchAirQuality(longitude, latitude);

        double humidity = parseDouble(nowNode.path("humidity"));
        double roundedHumidity = Double.isNaN(humidity) ? humidity : roundToSingleDecimal(humidity);
        double windSpeed = parseDouble(nowNode.path("windSpeed"));
        double windSpeedMeters = Double.isNaN(windSpeed) ? windSpeed : roundToSingleDecimal(windSpeed / 3.6);
        double precipitation = parseDouble(nowNode.path("precip"));
        double precipitationIntensity = Double.isNaN(precipitation) ? precipitation : roundToSingleDecimal(precipitation);
        String precipitationDescription = null;
        if (!Double.isNaN(precipitation) && precipitation > 0) {
            precipitationDescription = String.format("近一小时降水 %.1f 毫米", precipitationIntensity);
        }
        Instant fetchedAt = Optional.ofNullable(parseInstant(body.path("updateTime").asText(null))).orElse(Instant.now());

        return new WeatherRealtimeResponse(
            longitude,
            latitude,
            status,
            nowNode.path("text").asText(null),
            parseDouble(nowNode.path("temp")),
            parseDouble(nowNode.path("feelsLike")),
            roundedHumidity,
            parseDouble(nowNode.path("pressure")),
            parseDouble(nowNode.path("vis")),
            new WeatherRealtimeResponse.Wind(
                windSpeedMeters,
                parseDouble(nowNode.path("wind360"))
            ),
            new WeatherRealtimeResponse.Precipitation(
                precipitationIntensity,
                null,
                null
            ),
            airQuality,
            null,
            precipitationDescription,
            parseInstant(nowNode.path("obsTime").asText(null)),
            fetchedAt
        );
    }

    private WeatherRealtimeResponse.AirQuality fetchAirQuality(double longitude, double latitude) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather == null || !StringUtils.hasText(qweather.getKey())) {
            return null;
        }
        String baseUrl = Optional.ofNullable(qweather.getBaseUrl()).filter(StringUtils::hasText)
            .orElse("https://m776x8rde7.re.qweatherapi.com/v7");
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/air/now")
            .queryParam("location", resolveLocationParameter(longitude, latitude, qweather))
            .queryParam("lang", qweather.getLanguage())
            .queryParam("unit", qweather.getUnit());
        if (shouldAppendQueryKey(qweather)) {
            uriBuilder.queryParam("key", qweather.getKey());
        }
        URI requestUri = uriBuilder.build(true).toUri();

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.getForEntity(requestUri, JsonNode.class);
        } catch (HttpStatusCodeException ex) {
            log.debug("调用和风天气空气质量接口失败: {} {}", ex.getStatusCode(), safeBody(ex));
            return null;
        } catch (RestClientException ex) {
            log.debug("调用和风天气空气质量接口异常: {}", ex.getMessage());
            return null;
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }

        JsonNode body = response.getBody();
        if (!"200".equals(body.path("code").asText())) {
            return null;
        }

        JsonNode nowNode = body.path("now");
        if (nowNode == null || nowNode.isMissingNode() || nowNode.isNull()) {
            return null;
        }

        return new WeatherRealtimeResponse.AirQuality(
            nullableDouble(nowNode.path("aqi")),
            nullableDouble(nowNode.path("pm2p5")),
            nullableDouble(nowNode.path("pm10")),
            nullableDouble(nowNode.path("o3")),
            nullableDouble(nowNode.path("so2")),
            nullableDouble(nowNode.path("no2")),
            nullableDouble(nowNode.path("co")),
            nowNode.path("category").asText(null),
            nowNode.path("primary").asText(null)
        );
    }

    private String resolveLocationParameter(double longitude, double latitude, QWeatherProperties qweather) {
        LocationMode mode = Optional.ofNullable(qweather.getLocationMode()).orElse(LocationMode.COORDINATE);
        if (mode == LocationMode.GEO_LOOKUP) {
            Optional<String> locationId = locationClient.resolveLocationId(longitude, latitude);
            if (locationId.isPresent()) {
                return locationId.get();
            }
        }
        return longitude + "," + latitude;
    }

    private boolean shouldAppendQueryKey(QWeatherProperties qweather) {
        WeatherProperties.QWeatherProperties.AuthMode authMode = Optional.ofNullable(qweather.getAuthMode())
            .orElse(WeatherProperties.QWeatherProperties.AuthMode.QUERY_KEY);
        if (authMode == WeatherProperties.QWeatherProperties.AuthMode.QUERY_KEY) {
            return true;
        }
        return !StringUtils.hasText(qweather.getToken());
    }

    private BusinessException translateHttpException(String apiName, HttpStatusCodeException ex) {
        HttpStatusCode status = ex.getStatusCode();
        ResultCode code = mapStatusToResult(status);
        String body = safeBody(ex);
        String message = StringUtils.hasText(body)
            ? String.format("调用和风天气%s接口失败（%s %s）", apiName, status, body)
            : String.format("调用和风天气%s接口失败（%s）", apiName, status);
        return new BusinessException(code, message);
    }

    private ResultCode mapStatusToResult(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ResultCode.BAD_REQUEST;
            case 401 -> ResultCode.UNAUTHORIZED;
            case 403 -> ResultCode.FORBIDDEN;
            case 404 -> ResultCode.NOT_FOUND;
            default -> ResultCode.SERVER_ERROR;
        };
    }

    private String safeBody(HttpStatusCodeException ex) {
        try {
            return ex.getResponseBodyAsString();
        } catch (Exception ignore) {
            return "";
        }
    }

    private double parseDouble(JsonNode node) {
        Double value = nullableDouble(node);
        return value != null ? value : Double.NaN;
    }

    private Double nullableDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
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
