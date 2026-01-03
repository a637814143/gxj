package com.gxj.cropyield.modules.report.service;

import org.springframework.http.MediaType;

/**
 * 报表导出结果，包含导出字节、文件名与媒体类型信息。
 */
public record ReportExportResult(byte[] data, String filename, MediaType mediaType) {
}
