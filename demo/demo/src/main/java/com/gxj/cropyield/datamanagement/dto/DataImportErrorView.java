package com.gxj.cropyield.datamanagement.dto;
/**
 * 数据导入模块的数据传输对象（记录类型），在数据导入场景下承载参数与返回值。
 */

public record DataImportErrorView(
        int rowNumber,
        String errorCode,
        String message,
        String rawValue
) {
}
