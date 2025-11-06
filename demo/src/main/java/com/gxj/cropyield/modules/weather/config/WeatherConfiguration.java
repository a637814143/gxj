package com.gxj.cropyield.modules.weather.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 天气模块的配置类，负责构建调用第三方天气接口的 RestTemplate。
 */
@Configuration
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherConfiguration {

    @Bean({"weatherRestTemplate", "qweatherRestTemplate"})
    public RestTemplate qweatherRestTemplate(RestTemplateBuilder builder, WeatherProperties properties) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        Duration connectTimeout = qweather.getConnectTimeout();
        Duration readTimeout = qweather.getReadTimeout();
        return builder
            .setConnectTimeout(connectTimeout != null ? connectTimeout : Duration.ofSeconds(2))
            .setReadTimeout(readTimeout != null ? readTimeout : Duration.ofSeconds(5))
            .build();
    }
}
