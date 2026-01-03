package com.gxj.cropyield.modules.forecast.config;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 周期性扫描待处理的预测任务，推送到内部消息队列以解耦执行。
 */
@Component
public class ForecastDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(ForecastDispatchScheduler.class);

    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastTaskQueue forecastTaskQueue;

    public ForecastDispatchScheduler(ForecastTaskRepository forecastTaskRepository,
                                     ForecastTaskQueue forecastTaskQueue) {
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastTaskQueue = forecastTaskQueue;
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void dispatchPendingTasks() {
        List<ForecastTask> pendingTasks = forecastTaskRepository.findByStatus(ForecastTask.TaskStatus.PENDING);
        if (pendingTasks.isEmpty()) {
            return;
        }
        log.info("Dispatching {} pending forecast tasks", pendingTasks.size());
        pendingTasks.forEach(task -> {
            Map<String, Object> parameters = parseTaskParameters(task.getParameters());
            Integer forecastPeriods = parseInteger(parameters.get("forecastPeriods"));
            Integer historyYears = parseInteger(parameters.get("historyYears"));
            String frequency = parameters.getOrDefault("frequency", "YEAR").toString();
            forecastTaskQueue.publish(new ForecastExecutionRequest(
                    task.getRegion().getId(),
                    task.getCrop().getId(),
                    task.getModel().getId(),
                    forecastPeriods != null ? forecastPeriods : 3,
                    historyYears != null ? historyYears : 3,
                    frequency,
                    parameters.isEmpty() ? Collections.emptyMap() : parameters
            ));
            task.setStatus(ForecastTask.TaskStatus.RUNNING);
        });
        forecastTaskRepository.saveAll(pendingTasks);
    }

    private Map<String, Object> parseTaskParameters(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Object> parsed = new HashMap<>();
        for (String pair : parameters.split(";")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separatorIndex).trim();
            String value = trimmed.substring(separatorIndex + 1).trim();
            if (!key.isEmpty()) {
                parsed.put(key, value);
            }
        }
        return parsed;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse integer from task parameter '{}': {}", value, ex.getMessage());
            return null;
        }
    }
}
