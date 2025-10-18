package com.gxj.cropyield.modules.system.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.system.dto.SystemSettingRequest;
import com.gxj.cropyield.modules.system.dto.SystemSettingResponse;
import com.gxj.cropyield.modules.system.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping
    public ApiResponse<SystemSettingResponse> getSettings() {
        return ApiResponse.success(systemSettingService.getCurrentSettings());
    }

    @PutMapping
    public ApiResponse<SystemSettingResponse> updateSettings(@Valid @RequestBody SystemSettingRequest request) {
        return ApiResponse.success(systemSettingService.updateSettings(request));
    }
}
