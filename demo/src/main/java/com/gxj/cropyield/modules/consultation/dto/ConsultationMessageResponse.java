package com.gxj.cropyield.modules.consultation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线咨询模块的数据传输对象（记录类型），在返回会话消息时承载参数。
 */
public record ConsultationMessageResponse(
    Long id,
    Long consultationId,
    Long senderId,
    String senderName,
    String senderRole,
    String content,
    LocalDateTime createdAt,
    List<ConsultationAttachmentResponse> attachments
) {
}
