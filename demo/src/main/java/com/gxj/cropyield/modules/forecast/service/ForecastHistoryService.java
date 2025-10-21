package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryPageResponse;

public interface ForecastHistoryService {

    ForecastHistoryPageResponse getHistory(int page, int size);
}
