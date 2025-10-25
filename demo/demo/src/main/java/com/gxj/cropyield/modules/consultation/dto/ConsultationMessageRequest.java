package com.gxj.cropyield.modules.consultation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 在线咨询模块的数据传输对象（记录类型），在提交文字消息时承载参数。
 */
public record ConsultationMessageRequest(
    @NotBlank(message = "消息内容不能为空") String content
) {
}
