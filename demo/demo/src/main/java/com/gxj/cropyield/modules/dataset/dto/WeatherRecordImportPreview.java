package com.gxj.cropyield.modules.dataset.dto;

import java.time.LocalDate;

/**
 * 气象数据导入预览记录。
 */
public record WeatherRecordImportPreview(
        String regionName,
        LocalDate recordDate,
        Double maxTemperature,
        Double minTemperature,
        String weatherText,
        String wind,
        Double sunshineHours,
        String dataSource
) {
}
