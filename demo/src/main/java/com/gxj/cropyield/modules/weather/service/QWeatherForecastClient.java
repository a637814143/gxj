package com.gxj.cropyield.modules.weather.service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.gxj.cropyield.modules.dataset.entity.WeatherRecord;
import com.gxj.cropyield.modules.weather.config.WeatherProperties;

/**
 * 和风天气预报客户端，用于抓取逐日预报并转换成系统内部使用的气象记录结构。
 */
@Component
public class QWeatherForecastClient {

    private static final Logger log = LoggerFactory.getLogger(QWeatherForecastClient.class);

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;

    public QWeatherForecastClient(@Qualifier("qweatherRestTemplate") RestTemplate restTemplate,
                                  WeatherProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public List<WeatherRecord> fetchDailyForecast(double longitude, double latitude) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather == null || !StringUtils.hasText(qweather.getKey())) {
            log.debug("QWeather key is not configured, skip fetching forecast");
            return Collections.emptyList();
        }
        String baseUrl = Optional.ofNullable(qweather.getBaseUrl()).filter(StringUtils::hasText)
            .orElse("https://m776x8rde7.re.qweatherapi.com/v7");
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        URI requestUri = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/weather/15d")
            .queryParam("location", longitude + "," + latitude)
            .queryParam("key", qweather.getKey())
            .queryParam("lang", qweather.getLanguage())
            .queryParam("unit", qweather.getUnit())
            .build(true)
            .toUri();

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.getForEntity(requestUri, JsonNode.class);
        } catch (RestClientException ex) {
            log.warn("调用和风天气接口失败: {}", ex.getMessage());
            return Collections.emptyList();
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("和风天气返回非成功状态: {}", response.getStatusCode());
            return Collections.emptyList();
        }

        JsonNode root = response.getBody();
        String code = root.path("code").asText();
        if (!"200".equals(code)) {
            log.warn("和风天气返回错误码: {}", code);
            return Collections.emptyList();
        }

        JsonNode daily = root.path("daily");
        if (daily == null || !daily.isArray() || daily.isEmpty()) {
            return Collections.emptyList();
        }

        List<WeatherRecord> records = new ArrayList<>();
        for (JsonNode node : daily) {
            LocalDate date = parseDate(node.path("fxDate").asText(null));
            if (date == null) {
                continue;
            }
            WeatherRecord record = new WeatherRecord();
            record.setRecordDate(date);
            record.setMaxTemperature(parseDouble(node.path("tempMax")));
            record.setMinTemperature(parseDouble(node.path("tempMin")));
            record.setWeatherText(node.path("textDay").asText(null));
            record.setSunshineHours(computeSunshine(node.path("sunrise").asText(null), node.path("sunset").asText(null)));
            record.setDataSource("qweather-forecast");
            records.add(record);
        }
        return records;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            log.debug("和风天气日期解析失败: {}", value, ex);
            return null;
        }
    }

    private Double parseDouble(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        try {
            return Double.parseDouble(node.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double computeSunshine(String sunrise, String sunset) {
        if (!StringUtils.hasText(sunrise) || !StringUtils.hasText(sunset)) {
            return null;
        }
        try {
            LocalTime rise = LocalTime.parse(sunrise);
            LocalTime set = LocalTime.parse(sunset);
            if (set.isBefore(rise)) {
                return null;
            }
            Duration duration = Duration.between(rise, set);
            return duration.isNegative() ? null : duration.toMinutes() / 60d;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
