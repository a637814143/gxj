package com.gxj.cropyield.modules.forecast.config;

import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskQueue;
import java.util.List;
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
            forecastTaskQueue.publish(new ForecastExecutionRequest(
                    task.getRegion().getId(),
                    task.getCrop().getId(),
                    task.getModel().getId(),
                    3,
                    3,
                    "YEAR"
            ));
            task.setStatus(ForecastTask.TaskStatus.RUNNING);
        });
        forecastTaskRepository.saveAll(pendingTasks);
    }
}
