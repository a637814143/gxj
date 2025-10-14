package com.gxj.cropyield.modules.system.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.PageResponse;
import com.gxj.cropyield.modules.system.entity.SystemLog;
import com.gxj.cropyield.modules.system.service.SystemLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SystemLog>> listLogs(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> result = systemLogService.list(PageRequest.of(page, size));
        return ApiResponse.success(new PageResponse<>(result.getContent(), result.getTotalElements(), page, size));
    }
}
