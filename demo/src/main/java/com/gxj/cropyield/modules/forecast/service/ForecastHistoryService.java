package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryResponse;

import java.util.List;

public interface ForecastHistoryService {

    List<ForecastHistoryResponse> getRecentHistory(int limit);
}
