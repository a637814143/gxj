package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class InMemoryForecastTaskQueue implements ForecastTaskQueue {

    private final BlockingQueue<ForecastExecutionRequest> queue = new LinkedBlockingQueue<>();

    @Override
    public void publish(ForecastExecutionRequest request) {
        if (request != null) {
            queue.offer(request);
        }
    }

    @Override
    public ForecastExecutionRequest poll() {
        return queue.poll();
    }
}
