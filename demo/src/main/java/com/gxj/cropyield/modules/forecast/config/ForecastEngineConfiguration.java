package com.gxj.cropyield.modules.forecast.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 预测管理模块的配置类，配置预测管理相关的基础设施与框架行为。
 * <p>核心方法：forecastRestTemplate。</p>
 */
@Configuration
@EnableConfigurationProperties(ForecastEngineProperties.class)
public class ForecastEngineConfiguration {

    @Bean
    public RestTemplate forecastRestTemplate(RestTemplateBuilder builder, ForecastEngineProperties properties) {
        return builder
            .setConnectTimeout(properties.getConnectTimeout())
            .setReadTimeout(properties.getReadTimeout())
            .build();
    }
}
