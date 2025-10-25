package com.gxj.cropyield.modules.consultation.dto;

/**
 * 在线咨询模块的数据传输对象（记录类型），在返回附件信息时承载参数。
 */
public record ConsultationAttachmentResponse(
    Long id,
    String name,
    String url,
    String type
) {
}
