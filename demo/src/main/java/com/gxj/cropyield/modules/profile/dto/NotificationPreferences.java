package com.gxj.cropyield.modules.profile.dto;

public record NotificationPreferences(
    Boolean email,
    Boolean sms,
    Boolean inApp,
    Boolean security
) {
}
