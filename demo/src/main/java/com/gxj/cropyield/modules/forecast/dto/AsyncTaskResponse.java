package com.gxj.cropyield.modules.forecast.dto;

/**
 * 异步任务响应
 * 
 * @param taskId 任务ID
 * @param status 任务状态
 * @param message 提示信息
 */
public record AsyncTaskResponse(
    String taskId,
    String status,
    String message
) {}
