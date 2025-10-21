package com.gxj.cropyield.modules.profile.dto;

import java.time.LocalDateTime;

public record DeviceInfo(
    Long id,
    String name,
    String browser,
    String system,
    String location,
    LocalDateTime lastActive,
    Boolean trusted
) {
}
