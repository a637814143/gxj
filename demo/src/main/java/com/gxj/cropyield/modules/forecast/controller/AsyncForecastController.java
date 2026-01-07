package com.gxj.cropyield.modules.forecast.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.forecast.dto.AsyncTaskResponse;
import com.gxj.cropyield.modules.forecast.dto.AsyncTaskStatusResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastExecutionRequest;
import com.gxj.cropyield.modules.forecast.service.AsyncForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 异步预测控制器
 * 
 * 提供异步预测任务的提交、查询和取消功能
 */
/**
 * 异步预测任务控制器 - 异步执行预测、任务状态查询
 */
@RestController
@RequestMapping("/api/forecast/async")
public class AsyncForecastController {
    
    private final AsyncForecastService asyncForecastService;
    
    public AsyncForecastController(AsyncForecastService asyncForecastService) {
        this.asyncForecastService = asyncForecastService;
    }
    

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGRICULTURE_DEPT')")
    public ResponseEntity<ApiResponse<AsyncTaskResponse>> submitTask(
            @RequestBody ForecastExecutionRequest request) {
        AsyncTaskResponse response = asyncForecastService.submitForecastTask(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    

    @GetMapping("/status/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGRICULTURE_DEPT', 'FARMER')")
    public ResponseEntity<ApiResponse<AsyncTaskStatusResponse>> getTaskStatus(
            @PathVariable String taskId) {
        AsyncTaskStatusResponse response = asyncForecastService.getTaskStatus(taskId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGRICULTURE_DEPT')")
    public ResponseEntity<ApiResponse<String>> cancelTask(@PathVariable String taskId) {
        asyncForecastService.cancelTask(taskId);
        return ResponseEntity.ok(ApiResponse.success("任务已取消"));
    }
}
