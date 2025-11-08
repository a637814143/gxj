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
            if (!StringUtils.hasText(trimmedBase)) {
                continue;
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trimmedBase + "/city/lookup")
                .queryParam("location", longitude + "," + latitude);
            if (shouldAppendQueryKey(qweather)) {
                builder.queryParam("key", qweather.getKey());
            }
            URI requestUri = builder.build(true).toUri();

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

        addIfHasText(candidates, qweather.getGeoBaseUrl());
        for (String derived : deriveGeoBasesFromForecastBase(qweather.getBaseUrl())) {
            addIfHasText(candidates, derived);
        }
        candidates.add("https://devapi.qweather.com/geo/v2");
        candidates.add("https://geoapi.qweather.com/v2");

        return candidates;
    }

    private Iterable<String> deriveGeoBasesFromForecastBase(String baseUrl) {
        LinkedHashSet<String> derived = new LinkedHashSet<>();
        if (!StringUtils.hasText(baseUrl)) {
            return derived;
        }
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();
            if (!StringUtils.hasText(scheme)) {
                scheme = "https";
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return derived;
            }

            String root = scheme + "://" + host;
            derived.add(root + "/geo/v2");

            String geoHost = host;
            if (host.contains(".re.")) {
                geoHost = host.replaceFirst("\\.re\\.", ".geo.");
            } else if (host.startsWith("re.")) {
                geoHost = host.replaceFirst("^re\\.", "geo.");
            } else if (!host.contains(".geo.")) {
                int firstDot = host.indexOf('.');
                if (firstDot > 0) {
                    geoHost = host.substring(0, firstDot) + ".geo" + host.substring(firstDot);
                } else {
                    geoHost = "geo." + host;
                }
            }
            derived.add(scheme + "://" + geoHost + "/v2");
        } catch (IllegalArgumentException ex) {
            log.debug("无法从和风基础地址派生地理编码地址: {}", baseUrl, ex);
        }
        return derived;
    }

    private void addIfHasText(LinkedHashSet<String> set, String value) {
        if (StringUtils.hasText(value)) {
            set.add(value);
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean shouldAppendQueryKey(WeatherProperties.QWeatherProperties qweather) {
        WeatherProperties.QWeatherProperties.AuthMode authMode = Optional.ofNullable(qweather.getAuthMode())
            .orElse(WeatherProperties.QWeatherProperties.AuthMode.QUERY_KEY);
        if (authMode == WeatherProperties.QWeatherProperties.AuthMode.QUERY_KEY) {
            return true;
        }
        return !StringUtils.hasText(qweather.getToken());
    }

    private String safeBody(HttpStatusCodeException ex) {
        try {
            return ex.getResponseBodyAsString();
        } catch (Exception ignore) {
            return "";
        }
    }
}
