package com.gxj.cropyield.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 
 * 记录系统中所有重要操作的审计信息
 * 用于安全审计、问题排查和操作追踪
 */
@Entity
@Table(name = "sys_audit_log")
public class AuditLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作用户名
     */
    @Column(length = 100)
    private String username;
    
    /**
     * 操作类型
     * 例如：CREATE_USER, UPDATE_USER, DELETE_USER等
     */
    @Column(length = 50, nullable = false)
    private String operation;
    
    /**
     * 模块名称
     * 例如：用户管理、数据管理、预测管理等
     */
    @Column(length = 50, nullable = false)
    private String module;
    
    /**
     * 实体类型
     * 例如：User, YieldRecord, ForecastTask等
     */
    @Column(length = 100)
    private String entityType;
    
    /**
     * 实体ID
     */
    private Long entityId;
    
    /**
     * 操作描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * IP地址
     */
    @Column(length = 50)
    private String ipAddress;
    
    /**
     * 用户代理（浏览器信息）
     */
    @Column(length = 500)
    private String userAgent;
    
    /**
     * 请求URI
     */
    @Column(length = 200)
    private String requestUri;
    
    /**
     * 请求方法
     * GET, POST, PUT, DELETE等
     */
    @Column(length = 10)
    private String requestMethod;
    
    /**
     * 请求参数（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String requestParams;
    
    /**
     * 操作结果
     * SUCCESS, FAILURE
     */
    @Column(length = 20)
    private String result;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}
