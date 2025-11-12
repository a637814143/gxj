package com.gxj.cropyield.modules.weather.service.impl;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Locale;
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
import com.gxj.cropyield.modules.dataset.entity.WeatherRecord;
import com.gxj.cropyield.modules.weather.service.QWeatherForecastClient;
import com.gxj.cropyield.modules.weather.service.QWeatherLocationClient;
import com.gxj.cropyield.modules.weather.service.WeatherService;
import com.gxj.cropyield.modules.weather.service.dto.WeatherForecastResponse;
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
    private final QWeatherForecastClient forecastClient;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherServiceImpl(@Qualifier("weatherRestTemplate") RestTemplate weatherRestTemplate,
                              WeatherProperties properties,
                              QWeatherLocationClient locationClient,
                              QWeatherForecastClient forecastClient) {
        this.restTemplate = weatherRestTemplate;
        this.properties = properties;
        this.locationClient = locationClient;
        this.forecastClient = forecastClient;
    }

    @Override
    public WeatherForecastResponse getDailyForecast(double longitude, double latitude) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather == null || !StringUtils.hasText(qweather.getKey())) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "未配置和风天气访问密钥");
        }

        java.util.List<WeatherRecord> records = forecastClient.fetchDailyForecast(longitude, latitude);
        java.time.Instant fetchedAt = java.time.Instant.now();

        java.util.List<WeatherForecastResponse.DailyForecast> daily = records.stream()
            .map(record -> new WeatherForecastResponse.DailyForecast(
                record.getRecordDate(),
                record.getMaxTemperature(),
                record.getMinTemperature(),
                record.getWeatherText(),
                record.getSunshineHours(),
                record.getDataSource()
            ))
            .collect(java.util.stream.Collectors.toList());

        return new WeatherForecastResponse(fetchedAt, daily);
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
        String locationParam = resolveLocationParameter(longitude, latitude, qweather);
        String language = qweather.getLanguage();
        String unit = qweather.getUnit();
        BusinessException lastError = null;

        for (String base : buildWeatherBaseCandidates(qweather)) {
            String trimmedBase = trimTrailingSlash(base);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/weather/now")
                .queryParam("location", locationParam)
                .queryParam("lang", language)
                .queryParam("unit", unit);
            if (shouldAppendQueryKey(qweather)) {
                uriBuilder.queryParam("key", qweather.getKey());
            }
            URI requestUri = uriBuilder.build(true).toUri();

            ResponseEntity<JsonNode> response;
            try {
                response = restTemplate.getForEntity(requestUri, JsonNode.class);
            } catch (HttpStatusCodeException ex) {
                lastError = translateHttpException("实时", ex);
                continue;
            } catch (RestClientException ex) {
                lastError = new BusinessException(ResultCode.SERVER_ERROR,
                    "调用和风天气实时接口失败: " + ex.getMessage());
                continue;
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                lastError = new BusinessException(ResultCode.SERVER_ERROR,
                    "获取天气数据失败（" + trimmedBase + ")");
                continue;
            }

            JsonNode body = response.getBody();
            String status = body.path("code").asText();
            if (!"200".equals(status)) {
                String errorMessage = body.path("message").asText();
                if (!StringUtils.hasText(errorMessage)) {
                    errorMessage = body.path("refer").path("license").toString();
                }
                if (StringUtils.hasText(errorMessage)) {
                    lastError = new BusinessException(ResultCode.SERVER_ERROR,
                        "和风天气返回异常状态: " + errorMessage);
                } else {
                    lastError = new BusinessException(ResultCode.SERVER_ERROR,
                        "和风天气返回错误码: " + status);
                }
                continue;
            }

            JsonNode nowNode = body.path("now");
            WeatherRealtimeResponse.AirQuality airQuality = fetchAirQuality(locationParam, longitude, latitude, qweather);

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

        if (lastError != null) {
            throw lastError;
        }
        throw new BusinessException(ResultCode.SERVER_ERROR, "获取天气数据失败");
    }

    private WeatherRealtimeResponse.AirQuality fetchAirQuality(String locationParam,
                                                               double longitude,
                                                               double latitude,
                                                               QWeatherProperties qweather) {
        if (qweather == null) {
            return null;
        }

        WeatherRealtimeResponse.AirQuality hourly = fetchFromHourlyAirQuality(longitude, latitude, qweather);
        if (hourly != null) {
            return hourly;
        }

        if (!StringUtils.hasText(locationParam)) {
            locationParam = longitude + "," + latitude;
        }
        return fetchFromLegacyAirQuality(locationParam, qweather);
    }

    private WeatherRealtimeResponse.AirQuality fetchFromHourlyAirQuality(double longitude,
                                                                         double latitude,
                                                                         QWeatherProperties qweather) {
        BusinessException lastError = null;
        for (String base : buildAirQualityBaseCandidates(qweather)) {
            if (!StringUtils.hasText(base)) {
                continue;
            }
            String trimmedBase = trimTrailingSlash(base);
            String path = trimmedBase + "/airquality/v1/hourly/" + formatCoordinate(latitude)
                + "/" + formatCoordinate(longitude);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(path);
            if (StringUtils.hasText(qweather.getLanguage())) {
                uriBuilder.queryParam("lang", qweather.getLanguage());
            }
            if (shouldAppendQueryKey(qweather)) {
                uriBuilder.queryParam("key", qweather.getKey());
            }
            URI requestUri = uriBuilder.build(true).toUri();

            ResponseEntity<JsonNode> response;
            try {
                response = restTemplate.getForEntity(requestUri, JsonNode.class);
            } catch (HttpStatusCodeException ex) {
                lastError = translateHttpException("空气质量小时预报", ex);
                continue;
            } catch (RestClientException ex) {
                lastError = new BusinessException(ResultCode.SERVER_ERROR,
                    "调用空气质量小时预报接口失败: " + ex.getMessage());
                continue;
            }

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                continue;
            }

            WeatherRealtimeResponse.AirQuality parsed = parseHourlyAirQuality(response.getBody());
            if (parsed != null) {
                return parsed;
            }
        }

        if (lastError != null) {
            log.debug("调用空气质量小时预报接口失败，将回退旧接口: {}", lastError.getMessage());
        }
        return null;
    }

    private WeatherRealtimeResponse.AirQuality fetchFromLegacyAirQuality(String locationParam,
                                                                         QWeatherProperties qweather) {
        if (!StringUtils.hasText(qweather.getKey())) {
            return null;
        }
        BusinessException lastError = null;
        for (String base : buildWeatherBaseCandidates(qweather)) {
            String trimmedBase = trimTrailingSlash(base);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/air/now")
                .queryParam("location", locationParam)
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
                lastError = translateHttpException("空气质量", ex);
                continue;
            } catch (RestClientException ex) {
                lastError = new BusinessException(ResultCode.SERVER_ERROR,
                    "调用和风天气空气质量接口失败: " + ex.getMessage());
                continue;
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                continue;
            }

            JsonNode body = response.getBody();
            if (!"200".equals(body.path("code").asText())) {
                continue;
            }

            JsonNode nowNode = body.path("now");
            if (nowNode == null || nowNode.isMissingNode() || nowNode.isNull()) {
                continue;
            }

            return new WeatherRealtimeResponse.AirQuality(
                nullableDouble(nowNode.path("aqi")),
                nullableDouble(nowNode.path("pm2p5")),
                nullableDouble(nowNode.path("pm10")),
                nullableDouble(nowNode.path("o3")),
                nullableDouble(nowNode.path("so2")),
                nullableDouble(nowNode.path("no2")),
                nullableDouble(nowNode.path("co")),
                textOrNull(nowNode.path("category")),
                nowNode.path("primary").asText(null)
            );
        }

        if (lastError != null) {
            log.debug("调用空气质量接口失败，已忽略: {}", lastError.getMessage());
        }
        return null;
    }

    private WeatherRealtimeResponse.AirQuality parseHourlyAirQuality(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        JsonNode hours = payload.path("hours");
        if (hours == null || !hours.isArray() || hours.isEmpty()) {
            return null;
        }

        JsonNode selectedHour = selectBestHour(hours);
        if (selectedHour == null) {
            return null;
        }

        JsonNode indexNode = choosePreferredIndex(selectedHour.path("indexes"));
        Double aqi = indexNode != null ? nullableDouble(indexNode.path("aqi")) : nullableDouble(selectedHour.path("aqi"));
        String category = indexNode != null ? textOrNull(indexNode.path("category")) : textOrNull(selectedHour.path("category"));
        String description = extractHealthDescription(indexNode);
        if (!StringUtils.hasText(description)) {
            description = textOrNull(selectedHour.path("description"));
        }
        if (!StringUtils.hasText(description)) {
            description = category;
        }

        Double pm25 = extractPollutantConcentration(selectedHour, "pm2p5");
        Double pm10 = extractPollutantConcentration(selectedHour, "pm10");
        Double o3 = extractPollutantConcentration(selectedHour, "o3");
        Double so2 = extractPollutantConcentration(selectedHour, "so2");
        Double no2 = extractPollutantConcentration(selectedHour, "no2");
        Double co = extractPollutantConcentration(selectedHour, "co");

        if (aqi == null && pm25 == null && pm10 == null && o3 == null && so2 == null && no2 == null && co == null) {
            return null;
        }

        return new WeatherRealtimeResponse.AirQuality(aqi, pm25, pm10, o3, so2, no2, co, description, category);
    }

    private JsonNode selectBestHour(JsonNode hours) {
        Instant now = Instant.now();
        JsonNode bestNode = null;
        long bestDiff = Long.MAX_VALUE;
        for (JsonNode node : hours) {
            Instant forecastTime = parseInstant(node.path("forecastTime").asText(null));
            if (forecastTime == null) {
                if (bestNode == null) {
                    bestNode = node;
                }
                continue;
            }
            long diff = Math.abs(Duration.between(now, forecastTime).toMillis());
            if (diff < bestDiff) {
                bestDiff = diff;
                bestNode = node;
            }
        }
        return bestNode;
    }

    private JsonNode choosePreferredIndex(JsonNode indexesNode) {
        if (indexesNode == null || !indexesNode.isArray() || indexesNode.isEmpty()) {
            return null;
        }
        JsonNode fallback = null;
        for (JsonNode index : indexesNode) {
            if (fallback == null) {
                fallback = index;
            }
            String code = index.path("code").asText("").toLowerCase(Locale.ROOT);
            if ("qaqi".equals(code) || "aqi".equals(code)) {
                return index;
            }
        }
        return fallback;
    }

    private String extractHealthDescription(JsonNode indexNode) {
        if (indexNode == null || indexNode.isMissingNode() || indexNode.isNull()) {
            return null;
        }
        JsonNode health = indexNode.path("health");
        if (health != null && !health.isMissingNode() && !health.isNull()) {
            String effect = textOrNull(health.path("effect"));
            if (StringUtils.hasText(effect)) {
                return effect;
            }
            JsonNode advice = health.path("advice");
            if (advice != null && !advice.isMissingNode() && !advice.isNull()) {
                String general = textOrNull(advice.path("generalPopulation"));
                if (StringUtils.hasText(general)) {
                    return general;
                }
            }
        }
        return textOrNull(indexNode.path("name"));
    }

    private Double extractPollutantConcentration(JsonNode hourNode, String pollutantCode) {
        if (hourNode == null) {
            return null;
        }
        JsonNode pollutants = hourNode.path("pollutants");
        if (pollutants == null || !pollutants.isArray()) {
            return null;
        }
        for (JsonNode pollutant : pollutants) {
            if (pollutantCode.equalsIgnoreCase(pollutant.path("code").asText(""))) {
                return nullableDouble(pollutant.path("concentration").path("value"));
            }
        }
        return null;
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
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

    private Iterable<String> buildWeatherBaseCandidates(QWeatherProperties qweather) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (qweather != null && StringUtils.hasText(qweather.getBaseUrl())) {
            candidates.add(qweather.getBaseUrl());
        }
        candidates.add("https://devapi.qweather.com/v7");
        candidates.add("https://api.qweather.com/v7");
        return candidates;
    }

    private Iterable<String> buildAirQualityBaseCandidates(QWeatherProperties qweather) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (qweather == null) {
            return candidates;
        }
        if (StringUtils.hasText(qweather.getAirQualityBaseUrl())) {
            candidates.add(qweather.getAirQualityBaseUrl());
        }
        if (StringUtils.hasText(qweather.getBaseUrl())) {
            String trimmed = trimTrailingSlash(qweather.getBaseUrl());
            if (StringUtils.hasText(trimmed)) {
                candidates.add(trimmed);
                if (trimmed.endsWith("/v7")) {
                    String root = trimmed.substring(0, trimmed.length() - 3);
                    if (StringUtils.hasText(root)) {
                        candidates.add(trimTrailingSlash(root));
                    }
                }
            }
        }
        return candidates;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text : null;
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
