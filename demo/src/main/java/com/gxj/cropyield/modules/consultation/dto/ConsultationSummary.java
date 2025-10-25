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
    LocalDateTime closedAt,
    ConsultationMessageResponse lastMessage,
    long unreadCount,
    List<ConsultationParticipantResponse> participants,
    String description,
    Long ownerId,
    String ownerName,
    Long assignedToId,
    String assignedToName,
    long messageCount
) {
}
