package com.dali.cropyield.modules.forecast.service;

import com.dali.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastModel;
import java.util.List;

public interface ForecastModelService {

    List<ForecastModel> findAll();

    ForecastModel create(ForecastModelRequest request);
}
