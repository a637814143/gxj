package com.gxj.cropyield.modules.forecast.dto;

import java.util.List;
/**
 * 预测管理模块的数据传输对象（记录类型），在预测管理场景下承载参数与返回值。
 */

public record ForecastHistoryPageResponse(
    List<ForecastHistoryResponse> items,
    long total,
    int page,
    int size
) {
}
