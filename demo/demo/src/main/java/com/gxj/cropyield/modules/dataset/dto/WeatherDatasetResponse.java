package com.gxj.cropyield.modules.dataset.dto;

import java.util.List;

/**
 * 气象数据查询响应。
 */
public record WeatherDatasetResponse(
        WeatherDatasetSummary summary,
        List<WeatherRecordView> records
) {
}
