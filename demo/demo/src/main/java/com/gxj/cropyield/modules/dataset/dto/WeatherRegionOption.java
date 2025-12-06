package com.gxj.cropyield.modules.dataset.dto;

/**
 * 提供拥有有效气象记录的地区选项。
 */
public record WeatherRegionOption(
        Long regionId,
        String regionName,
        long recordCount
) {
}
