package com.gxj.cropyield.modules.profile.dto;

public record PersonalizationSettings(
    String theme,
    String accentColor,
    String density,
    String digestFrequency,
    Boolean quietMode,
    NotificationPreferences notifications
) {
}
