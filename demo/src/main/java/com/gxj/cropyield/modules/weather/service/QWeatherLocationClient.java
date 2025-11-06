package com.gxj.cropyield.modules.weather.service;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.gxj.cropyield.modules.weather.config.WeatherProperties;

/**
 * 和风天气地理位置客户端，根据经纬度解析对应的 LocationId。
 */
@Component
public class QWeatherLocationClient {

    private static final Logger log = LoggerFactory.getLogger(QWeatherLocationClient.class);

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;

    public QWeatherLocationClient(@Qualifier("qweatherRestTemplate") RestTemplate restTemplate,
                                  WeatherProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<String> resolveLocationId(double longitude, double latitude) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        if (qweather == null || !StringUtils.hasText(qweather.getKey())) {
            return Optional.empty();
        }
        for (String base : buildGeoBaseCandidates(qweather)) {
            String trimmedBase = trimTrailingSlash(base);

            URI requestUri = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/city/lookup")
                .queryParam("location", longitude + "," + latitude)
                .queryParam("key", qweather.getKey())
                .build(true)
                .toUri();

            ResponseEntity<JsonNode> response;
            try {
                response = restTemplate.getForEntity(requestUri, JsonNode.class);
            } catch (HttpStatusCodeException ex) {
                log.warn("和风天气地理编码接口返回异常状态 ({}): {} {}", trimmedBase, ex.getStatusCode(),
                    safeBody(ex));
                continue;
            } catch (RestClientException ex) {
                log.warn("调用和风天气地理编码接口失败 ({}): {}", trimmedBase, ex.getMessage());
                continue;
            }

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                continue;
            }

            JsonNode root = response.getBody();
            if (!"200".equals(root.path("code").asText())) {
                log.warn("和风天气地理编码返回错误码 ({}): {}", trimmedBase, root.path("code").asText());
                continue;
            }

            JsonNode locationArray = root.path("location");
            if (locationArray == null || !locationArray.isArray() || locationArray.isEmpty()) {
                continue;
            }

            JsonNode first = locationArray.get(0);
            String id = first.path("id").asText(null);
            if (!StringUtils.hasText(id)) {
                continue;
            }
            return Optional.of(id);
        }

        return Optional.empty();
    }

    private Iterable<String> buildGeoBaseCandidates(WeatherProperties.QWeatherProperties qweather) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        if (StringUtils.hasText(qweather.getGeoBaseUrl())) {
            candidates.add(qweather.getGeoBaseUrl());
        }

        String derived = deriveGeoBaseFromForecastBase(qweather.getBaseUrl());
        if (StringUtils.hasText(derived)) {
            candidates.add(derived);
        }

        candidates.add("https://geoapi.qweather.com/v2");

        return candidates;
    }

    private String deriveGeoBaseFromForecastBase(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        try {
            String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!StringUtils.hasText(scheme)) {
                scheme = "https";
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }
            String derivedHost;
            if (host.contains(".re.")) {
                derivedHost = host.replaceFirst("\\.re\\.", ".geo.");
            } else if (host.startsWith("re.")) {
                derivedHost = host.replaceFirst("^re\\.", "geo.");
            } else if (!host.contains(".geo.")) {
                derivedHost = host.replaceFirst("\\.", ".geo.");
            } else {
                derivedHost = host;
            }
            return scheme + "://" + derivedHost + "/v2";
        } catch (IllegalArgumentException ex) {
            log.debug("无法从和风基础地址派生地理编码地址: {}", baseUrl, ex);
            return null;
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safeBody(HttpStatusCodeException ex) {
        try {
            return ex.getResponseBodyAsString();
        } catch (Exception ignore) {
            return "";
        }
    }
}
