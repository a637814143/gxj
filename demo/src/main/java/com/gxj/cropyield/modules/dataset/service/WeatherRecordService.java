package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.WeatherCropOption;
import com.gxj.cropyield.modules.dataset.dto.WeatherDatasetResponse;
import com.gxj.cropyield.modules.dataset.dto.WeatherRegionOption;

import java.time.LocalDate;
import java.util.List;

/**
 * 气象数据服务定义。
 */
public interface WeatherRecordService {

    WeatherDatasetResponse queryDataset(Long regionId, LocalDate startDate, LocalDate endDate, Integer limit);

    List<WeatherRegionOption> listRegionsWithRecords();

    List<WeatherCropOption> listCropsWithRecords(Long regionId);
}
