package com.gxj.cropyield.modules.weather.config;

import java.time.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 天气模块的配置类，负责构建调用第三方天气接口的 RestTemplate。
 */
@Configuration
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WeatherConfiguration.class);
    private static final AtomicBoolean BEARER_TOKEN_MISSING_LOGGED = new AtomicBoolean(false);

    @Bean({"weatherRestTemplate", "qweatherRestTemplate"})
    public RestTemplate qweatherRestTemplate(RestTemplateBuilder builder, WeatherProperties properties) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        Duration connectTimeout = qweather.getConnectTimeout();
        Duration readTimeout = qweather.getReadTimeout();
        return builder
            .setConnectTimeout(connectTimeout != null ? connectTimeout : Duration.ofSeconds(2))
            .setReadTimeout(readTimeout != null ? readTimeout : Duration.ofSeconds(5))
            .additionalInterceptors((request, body, execution) -> {
                HttpHeaders headers = request.getHeaders();
                if (StringUtils.hasText(qweather.getReferer()) && !headers.containsKey(HttpHeaders.REFERER)) {
                    headers.set(HttpHeaders.REFERER, qweather.getReferer());
                }
                if (StringUtils.hasText(qweather.getUserAgent()) && !headers.containsKey(HttpHeaders.USER_AGENT)) {
                    headers.set(HttpHeaders.USER_AGENT, qweather.getUserAgent());
                }
                if (qweather.getAuthMode() == WeatherProperties.QWeatherProperties.AuthMode.HEADER_BEARER
                    && !headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    String token = qweather.getToken();
                    if (StringUtils.hasText(token)) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    } else if (StringUtils.hasText(qweather.getKey())
                        && BEARER_TOKEN_MISSING_LOGGED.compareAndSet(false, true)) {
                        log.warn("和风天气配置为 Bearer 模式，但未设置 token，将回退到 query key 方式");
                    }
                }
                return execution.execute(request, body);
            })
            .build();
    }
}
