package com.gxj.cropyield.modules.consultation.dto;

/**
 * 在线咨询模块的数据传输对象（记录类型），在更新咨询元信息场景下承载参数。
 */
public record ConsultationUpdateRequest(
    String status,
    String priority,
    Long assignedTo
) {
}
