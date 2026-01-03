package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ModelRegistry;

public interface ModelRegistryService {

    ModelRegistry registerSnapshot(ForecastModel model, String storageUri, String metricsJson);
}
