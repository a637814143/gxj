package com.gxj.cropyield.modules.consultation.dto;

import java.util.List;

/**
 * 在线咨询模块的数据传输对象（记录类型），在返回会话消息列表时承载参数。
 */
public record ConsultationMessagesResponse(
    ConsultationSummary conversation,
    List<ConsultationMessageResponse> items,
    long total
) {
}
