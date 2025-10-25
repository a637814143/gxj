package com.gxj.cropyield.modules.consultation.dto;

import org.springframework.core.io.Resource;

/**
 * 在线咨询模块的数据传输对象（记录类型），在下载附件时承载文件资源。
 */
public record ConsultationAttachmentDownload(
    Resource resource,
    String filename,
    String contentType
) {
}
