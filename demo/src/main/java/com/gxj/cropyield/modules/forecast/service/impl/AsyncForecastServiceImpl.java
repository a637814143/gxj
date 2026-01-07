package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.audit.AuditLog;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.forecast.dto.AsyncTaskResponse;
import com.gxj.cropyield.modules.forecast.dto.AsyncTaskStatusResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionResponse;
import com.gxj.cropyield.modules.forecast.entity.AsyncForecastTask;
import com.gxj.cropyield.modules.forecast.repository.AsyncForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.AsyncForecastService;
import com.gxj.cropyield.modules.forecast.service.ForecastExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 异步预测任务服务实现类 - 异步执行预测任务
 */
@Service
public class AsyncForecastServiceImpl implements AsyncForecastService {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncForecastServiceImpl.class);
    
    private final AsyncForecastTaskRepository asyncTaskRepository;
    private final ForecastExecutionService forecastExecutionService;
    
    public AsyncForecastServiceImpl(
            AsyncForecastTaskRepository asyncTaskRepository,
            ForecastExecutionService forecastExecutionService) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.forecastExecutionService = forecastExecutionService;
    }
    
    @Override
    @Transactional
    @AuditLog(operation = "SUBMIT_ASYNC_FORECAST", module = "预测管理", description = "提交异步预测任务")
    public AsyncTaskResponse submitForecastTask(ForecastExecutionRequest request) {
        log.info("提交异步预测任务 - 模型: {}, 作物: {}, 区域: {}", 
                request.modelId(), request.cropId(), request.regionId());
        
        // 生成任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 创建任务记录
        AsyncForecastTask task = new AsyncForecastTask();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        task.setTaskType("FORECAST");
        task.setModelId(request.modelId());
        task.setCropId(request.cropId());
        task.setRegionId(request.regionId());
        task.setTargetYear(java.time.LocalDate.now().getYear() + 1); // 默认预测下一年
        task.setProgress(0);
        task.setCurrentStep("等待执行");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        asyncTaskRepository.save(task);
        
        // 异步执行预测任务
        executeForecastAsync(taskId, request);
        
        log.info("异步预测任务已提交 - 任务ID: {}", taskId);
        
        return new AsyncTaskResponse(
            taskId,
            "PENDING",
            "预测任务已提交，正在处理中"
        );
    }
    
    /**
     * 异步执行预测任务
     * 
     * 使用@Async注解，在独立线程中执行
     * 使用forecastExecutor线程池
     */
    @Async("forecastExecutor")
    public void executeForecastAsync(String taskId, ForecastExecutionRequest request) {
        log.info("开始执行异步预测任务 - 任务ID: {}", taskId);
        
        AsyncForecastTask task = asyncTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "任务不存在"));
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 更新任务状态为运行中
            updateTaskStatus(task, "RUNNING", 10, "加载历史数据");
            
            // 执行预测
            log.debug("执行预测 - 任务ID: {}", taskId);
            updateTaskStatus(task, "RUNNING", 30, "执行预测模型");
            
            ForecastExecutionResponse response = forecastExecutionService.runForecast(request);
            
            // 更新任务状态为完成
            updateTaskStatus(task, "RUNNING", 90, "保存预测结果");
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCurrentStep("预测完成");
            task.setResultId(response.runId());
            task.setEndTime(LocalDateTime.now());
            task.setExecutionTime(executionTime);
            task.setUpdatedAt(LocalDateTime.now());
            asyncTaskRepository.save(task);
            
            log.info("异步预测任务完成 - 任务ID: {}, 结果ID: {}, 耗时: {}ms", 
                    taskId, response.runId(), executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.error("异步预测任务失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            
            task.setStatus("FAILED");
            task.setProgress(0);
            task.setCurrentStep("执行失败");
            task.setErrorMessage(e.getMessage());
            task.setEndTime(LocalDateTime.now());
            task.setExecutionTime(executionTime);
            task.setUpdatedAt(LocalDateTime.now());
            asyncTaskRepository.save(task);
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(AsyncForecastTask task, String status, int progress, String step) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setCurrentStep(step);
        task.setUpdatedAt(LocalDateTime.now());
        
        if ("RUNNING".equals(status) && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        
        asyncTaskRepository.save(task);
        log.debug("任务状态更新 - 任务ID: {}, 进度: {}%, 步骤: {}", 
                task.getTaskId(), progress, step);
    }
    
    @Override
    @Transactional(readOnly = true)
    public AsyncTaskStatusResponse getTaskStatus(String taskId) {
        log.debug("查询任务状态 - 任务ID: {}", taskId);
        
        AsyncForecastTask task = asyncTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "任务不存在"));
        
        return new AsyncTaskStatusResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getTaskType(),
            task.getProgress(),
            task.getCurrentStep(),
            task.getResultId(),
            task.getErrorMessage(),
            task.getStartTime(),
            task.getEndTime(),
            task.getExecutionTime()
        );
    }
    
    @Override
    @Transactional
    @AuditLog(operation = "CANCEL_ASYNC_FORECAST", module = "预测管理", description = "取消异步预测任务")
    public void cancelTask(String taskId) {
        log.info("取消异步预测任务 - 任务ID: {}", taskId);
        
        AsyncForecastTask task = asyncTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "任务不存在"));
        
        if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "任务已完成或失败，无法取消");
        }
        
        task.setStatus("CANCELLED");
        task.setCurrentStep("已取消");
        task.setEndTime(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        asyncTaskRepository.save(task);
        
        log.info("异步预测任务已取消 - 任务ID: {}", taskId);
    }
}
