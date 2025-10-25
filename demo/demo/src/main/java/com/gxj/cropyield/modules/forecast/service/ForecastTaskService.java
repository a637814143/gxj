package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;

import java.util.List;
/**
 * 预测管理模块的业务接口（接口），定义预测管理相关的核心业务操作。
 * <p>核心方法：listAll、create。</p>
 */

public interface ForecastTaskService {

    List<ForecastTask> listAll();

    ForecastTask create(ForecastTaskRequest request);
}
