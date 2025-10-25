package com.gxj.cropyield.modules.consultation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线咨询模块的数据传输对象（记录类型），在返回会话概览时承载参数。
 */
public record ConsultationSummary(
    Long id,
    String subject,
    String cropType,
    String status,
    String priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ConsultationMessageResponse lastMessage,
    long unreadCount,
    List<ConsultationParticipantResponse> participants
) {
}
