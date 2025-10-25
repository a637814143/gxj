package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.config.ForecastEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
/**
 * 预测管理模块的业务组件，封装预测管理的算法或执行流程。
 */

@Component
public class ForecastEngineClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastEngineClient.class);

    private final RestTemplate restTemplate;
    private final ForecastEngineProperties properties;
    private final LocalForecastEngine localForecastEngine;

    public ForecastEngineClient(@Qualifier("forecastRestTemplate") RestTemplate forecastRestTemplate,
                                ForecastEngineProperties properties,
                                LocalForecastEngine localForecastEngine) {
        this.restTemplate = forecastRestTemplate;
        this.properties = properties;
        this.localForecastEngine = localForecastEngine;
    }

    public ForecastEngineResponse runForecast(ForecastEngineRequest request) {
        if (StringUtils.hasText(properties.getBaseUrl())) {
            try {
                ResponseEntity<ForecastEngineResponse> response = restTemplate.postForEntity(
                    properties.getBaseUrl(), request, ForecastEngineResponse.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
                log.warn("模型服务返回非成功状态: {}", response.getStatusCode());
            } catch (RestClientException ex) {
                log.warn("调用外部模型服务失败，将使用内置回退逻辑: {}", ex.getMessage());
            }
        }
        return localForecastEngine.forecast(request);
    }
}
