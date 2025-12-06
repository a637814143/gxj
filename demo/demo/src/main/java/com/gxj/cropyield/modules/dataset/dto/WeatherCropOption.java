package com.gxj.cropyield.modules.dataset.dto;

/**
 * 提供拥有有效气象相关记录的作物选项。
 */
public record WeatherCropOption(
        Long cropId,
        String cropName,
        long recordCount
) {
}
