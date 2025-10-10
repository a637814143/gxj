package com.gxj.cropyield.modules.forecast.service;

import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;

import java.util.List;

public interface ForecastModelService {

    List<ForecastModel> listAll();

    ForecastModel create(ForecastModelRequest request);
}
