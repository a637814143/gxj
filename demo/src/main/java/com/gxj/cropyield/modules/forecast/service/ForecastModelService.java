package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;

import java.util.List;
/**
 * 预测管理模块的业务接口（接口），定义预测管理相关的核心业务操作。
 * <p>核心方法：listAll、create。</p>
 */

public interface ForecastModelService {

    List<ForecastModel> listAll();

    List<ForecastModel> listAvailable();

    ForecastModel create(ForecastModelRequest request);

    ForecastModel update(Long id, ForecastModelRequest request);

    ForecastModel duplicate(Long id);

    ForecastModel toggleEnabled(Long id, boolean enabled);
}
