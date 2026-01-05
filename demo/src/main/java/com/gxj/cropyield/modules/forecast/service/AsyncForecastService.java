package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.AsyncTaskResponse;
import com.gxj.cropyield.modules.forecast.dto.AsyncTaskStatusResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;

/**
 * 异步预测服务接口
 * 
 * 提供异步执行预测任务的功能
 * 避免长时间运行的LSTM训练阻塞请求
 */
public interface AsyncForecastService {
    
    /**
     * 异步执行预测任务
     * 
     * @param request 预测请求
     * @return 任务ID和初始状态
     */
    AsyncTaskResponse submitForecastTask(ForecastExecutionRequest request);
    
    /**
     * 查询任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    AsyncTaskStatusResponse getTaskStatus(String taskId);
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     */
    void cancelTask(String taskId);
}
