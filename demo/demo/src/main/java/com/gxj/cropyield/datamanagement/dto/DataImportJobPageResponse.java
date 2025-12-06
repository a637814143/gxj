package com.gxj.cropyield.datamanagement.dto;

import java.util.List;
/**
 * 数据导入模块的数据传输对象（记录类型），在数据导入场景下承载参数与返回值。
 */

public record DataImportJobPageResponse(
        List<DataImportJobView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
