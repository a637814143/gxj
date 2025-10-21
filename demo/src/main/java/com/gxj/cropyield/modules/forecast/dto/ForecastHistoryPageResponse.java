package com.gxj.cropyield.modules.forecast.dto;

import java.util.List;

public record ForecastHistoryPageResponse(
    List<ForecastHistoryResponse> items,
    long total,
    int page,
    int size
) {
}
