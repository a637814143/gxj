package com.gxj.cropyield.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
/**
 * 全局配置模块的配置类，配置全局配置相关的基础设施与框架行为。
 * <p>核心方法：importTaskExecutor。</p>
 */

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "importTaskExecutor")
    public TaskExecutor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("import-task-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "mailTaskExecutor")
    public TaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-task-");
        executor.initialize();
        return executor;
    }
}
