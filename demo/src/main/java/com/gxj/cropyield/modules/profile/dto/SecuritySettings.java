package com.gxj.cropyield.modules.profile.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SecuritySettings(
    LocalDateTime lastLoginAt,
    LocalDateTime lastPasswordChange,
    List<DeviceInfo> devices
) {
}
