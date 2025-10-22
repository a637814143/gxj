package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryPageResponse;
/**
 * 预测管理模块的业务接口（接口），定义预测管理相关的核心业务操作。
 * <p>核心方法：getHistory、deleteHistory。</p>
 */

public interface ForecastHistoryService {

    ForecastHistoryPageResponse getHistory(int page, int size);

    void deleteHistory(Long runId);
}
