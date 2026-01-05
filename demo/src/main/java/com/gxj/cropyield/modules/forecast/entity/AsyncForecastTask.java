package com.gxj.cropyield.modules.forecast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 异步预测任务实体
 * 
 * 用于跟踪异步执行的预测任务状态
 * 支持长时间运行的LSTM训练等操作
 */
@Entity
@Table(name = "async_forecast_task")
public class AsyncForecastTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 任务ID（UUID）
     */
    @Column(unique = true, nullable = false, length = 36)
    private String taskId;
    
    /**
     * 任务状态
     * PENDING - 等待执行
     * RUNNING - 执行中
     * COMPLETED - 已完成
     * FAILED - 失败
     * CANCELLED - 已取消
     */
    @Column(nullable = false, length = 20)
    private String status;
    
    /**
     * 任务类型
     * FORECAST - 预测任务
     * IMPORT - 数据导入
     * EXPORT - 数据导出
     */
    @Column(nullable = false, length = 20)
    private String taskType;
    
    /**
     * 模型ID
     */
    private Long modelId;
    
    /**
     * 作物ID
     */
    private Long cropId;
    
    /**
     * 区域ID
     */
    private Long regionId;
    
    /**
     * 目标年份
     */
    private Integer targetYear;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
    
    /**
     * 当前步骤描述
     */
    @Column(length = 200)
    private String currentStep;
    
    /**
     * 结果ID（ForecastRun ID）
     */
    private Long resultId;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行时长（毫秒）
     */
    private Long executionTime;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
