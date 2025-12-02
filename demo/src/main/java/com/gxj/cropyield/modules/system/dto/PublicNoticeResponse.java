package com.gxj.cropyield.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 对外公共通知的数据传输对象，仅暴露可展示的信息。
 */
public record PublicNoticeResponse(
    boolean visible,
    String title,
    String summary,
    String audience,
    String level,
    LocalDateTime startAt,
    LocalDateTime endAt,
    LocalDateTime updatedAt
) {
}
