package com.gxj.cropyield.modules.dataset.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 数据集管理模块的数据传输对象（记录类型），在数据集管理场景下承载参数与返回值。
 */

public record YieldRecordDetailView(
        Long id,
        CropInfo crop,
        RegionInfo region,
        Integer year,
        Double sownArea,
        Double production,
        Double yieldPerHectare,
        Double averagePrice,
        String dataSource,
        LocalDate collectedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record CropInfo(
            Long id,
            String code,
            String name,
            String category,
            String description
    ) {
    }

    public record RegionInfo(
            Long id,
            String code,
            String name,
            String level,
            String parentCode,
            String parentName,
            String description
    ) {
    }
}
