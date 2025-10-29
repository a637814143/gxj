package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.WeatherDatasetResponse;

import java.time.LocalDate;

/**
 * 气象数据服务定义。
 */
public interface WeatherRecordService {

    WeatherDatasetResponse queryDataset(Long regionId, LocalDate startDate, LocalDate endDate, Integer limit);
}
