package com.gxj.cropyield.modules.consultation.dto;

import java.util.List;

/**
 * 在线咨询模块的数据传输对象（记录类型），在分页返回会话列表时承载参数。
 */
public record ConsultationPageResponse(
    List<ConsultationSummary> items,
    long total,
    int page,
    int pageSize
) {
}
