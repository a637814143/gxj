package com.gxj.cropyield.modules.dataset.dto;

import java.time.LocalDate;
/**
 * 数据集管理模块的数据传输对象（记录类型），在数据集管理场景下承载参数与返回值。
 */

public record YieldRecordResponse(
        Long id,
        String cropName,
        String cropCategory,
        String regionName,
        String regionLevel,
        Integer year,
        Double sownArea,
        Double production,
        Double yieldPerHectare,
        String dataSource,
        LocalDate collectedAt
) {
}
