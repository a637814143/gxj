package com.gxj.cropyield.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 
 * 用于处理耗时较长的操作，如LSTM模型训练、数据导入等
 * 避免阻塞主线程，提升系统响应速度
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);
    
    /**
     * 配置异步任务执行器
     * 
     * 线程池配置：
     * - 核心线程数：5（常驻线程）
     * - 最大线程数：10（高峰期最多线程）
     * - 队列容量：100（等待队列）
     * - 线程空闲时间：60秒
     * - 拒绝策略：CallerRunsPolicy（由调用线程执行）
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：系统启动时创建的线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数：系统最多可以创建的线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量：等待队列的大小
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("async-forecast-");
        
        // 线程空闲时间：超过核心线程数的线程在空闲时的存活时间
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：当队列满时，由调用线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：最多等待60秒
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("异步任务执行器初始化完成 - 核心线程: {}, 最大线程: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 配置预测任务专用执行器
     * 
     * 用于LSTM等耗时较长的预测任务
     * 线程池较小，避免占用过多资源
     */
    @Bean(name = "forecastExecutor")
    public Executor getForecastExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 预测任务通常耗时较长，使用较小的线程池
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("forecast-task-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.initialize();
        
        log.info("预测任务执行器初始化完成 - 核心线程: {}, 最大线程: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 配置数据导入专用执行器
     * 
     * 用于大批量数据导入任务
     */
    @Bean(name = "importExecutor")
    public Executor getImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("import-task-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("数据导入执行器初始化完成 - 核心线程: {}, 最大线程: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 配置邮件发送专用执行器
     * 
     * 用于异步发送邮件（密码重置、通知等）
     */
    @Bean(name = "mailTaskExecutor")
    public Executor getMailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 邮件发送通常很快，使用较小的线程池
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-task-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("邮件任务执行器初始化完成 - 核心线程: {}, 最大线程: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 异步任务异常处理器
     * 
     * 捕获异步任务中未处理的异常，记录日志
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常 - 方法: {}, 参数: {}, 错误: {}", 
                    method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
