package com.gxj.cropyield.modules.system.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.system.dto.PublicNoticeResponse;
import com.gxj.cropyield.modules.system.service.SystemSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外公开的系统设置接口，向非管理员用户提供通知信息。
 */
@RestController
@RequestMapping("/api/system/public-settings")
public class PublicSystemSettingController {

    private final SystemSettingService systemSettingService;

    public PublicSystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping
    public ApiResponse<PublicNoticeResponse> getPublicNotice() {
        return ApiResponse.success(systemSettingService.getPublicNotice());
    }
}
