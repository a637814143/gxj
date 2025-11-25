package com.gxj.cropyield.modules.notification.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.notification.dto.EmailNotificationRequest;
import com.gxj.cropyield.modules.notification.service.EmailNotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;

    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/email")
    public ApiResponse<Void> sendEmail(@Valid @RequestBody EmailNotificationRequest request) {
        emailNotificationService.sendEmailNotification(request);
        return ApiResponse.success();
    }
}
