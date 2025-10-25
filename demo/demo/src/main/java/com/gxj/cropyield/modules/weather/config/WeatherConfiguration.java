package com.gxj.cropyield.modules.weather.config;

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

    @Bean
    public RestTemplate weatherRestTemplate(RestTemplateBuilder builder, WeatherProperties properties) {
        WeatherProperties.CaiyunProperties caiyun = properties.getCaiyun();
        return builder
            .setConnectTimeout(caiyun.getConnectTimeout())
            .setReadTimeout(caiyun.getReadTimeout())
            .build();
    }
}
