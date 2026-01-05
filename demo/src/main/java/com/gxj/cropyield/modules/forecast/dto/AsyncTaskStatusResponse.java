package com.gxj.cropyield.modules.forecast.dto;

import java.time.LocalDateTime;

/**
 * 异步任务状态响应
 * 
 * @param taskId 任务ID
 * @param status 任务状态
 * @param taskType 任务类型
 * @param progress 进度百分比
 * @param currentStep 当前步骤
 * @param resultId 结果ID
 * @param errorMessage 错误信息
 * @param startTime 开始时间
 * @param endTime 完成时间
 * @param executionTime 执行时长（毫秒）
 */
public record AsyncTaskStatusResponse(
    String taskId,
    String status,
    String taskType,
    Integer progress,
    String currentStep,
    Long resultId,
    String errorMessage,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Long executionTime
) {}
