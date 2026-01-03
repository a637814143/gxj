package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.service.ForecastExecutionService;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消费内部消息队列的任务并触发预测执行。
 */
@Component
public class ForecastTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(ForecastTaskWorker.class);

    private final ForecastTaskQueue forecastTaskQueue;
    private final ForecastExecutionService forecastExecutionService;

    public ForecastTaskWorker(ForecastTaskQueue forecastTaskQueue,
                              ForecastExecutionService forecastExecutionService) {
        this.forecastTaskQueue = forecastTaskQueue;
        this.forecastExecutionService = forecastExecutionService;
    }

    @Scheduled(fixedDelay = 10000)
    public void consume() {
        ForecastExecutionRequest request;
        while ((request = forecastTaskQueue.poll()) != null) {
            try {
                forecastExecutionService.runForecast(request);
            } catch (Exception ex) {
                log.error("Failed to execute scheduled forecast task", ex);
            }
        }
    }
}
