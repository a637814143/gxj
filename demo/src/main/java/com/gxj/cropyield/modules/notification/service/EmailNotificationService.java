package com.gxj.cropyield.modules.notification.service;

import com.gxj.cropyield.modules.notification.dto.EmailNotificationRequest;

public interface EmailNotificationService {

    void sendEmailNotification(EmailNotificationRequest request);
}
