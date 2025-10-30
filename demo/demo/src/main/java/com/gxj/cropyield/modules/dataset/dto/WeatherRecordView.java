package com.gxj.cropyield.modules.dataset.dto;

import java.time.LocalDate;

/**
 * 气象数据对外展示模型。
 */
public record WeatherRecordView(
        Long id,
        Long regionId,
        String regionName,
        LocalDate recordDate,
        Double maxTemperature,
        Double minTemperature,
        String weatherText,
        String wind,
        Double sunshineHours,
        String dataSource,
        String datasetName
) {
}
