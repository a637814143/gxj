package com.gxj.cropyield.modules.consultation.dto;

/**
 * 在线咨询模块的数据传输对象（记录类型），在返回会话参与者信息时承载参数。
 */
public record ConsultationParticipantResponse(
    Long userId,
    String name,
    String role,
    boolean owner,
    boolean self
) {
}
