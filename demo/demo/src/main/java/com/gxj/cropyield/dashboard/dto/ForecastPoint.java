package com.gxj.cropyield.dashboard.dto;
/**
 * 驾驶舱统计模块的数据传输对象（记录类型），在驾驶舱统计场景下承载参数与返回值。
 */

public record ForecastPoint(
        String label,
        double value,
        double lowerBound,
        double upperBound,
        String model
) {
}
