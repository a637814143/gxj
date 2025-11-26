package com.gxj.cropyield.modules.forecast.dto;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.Set;

public record ForecastModelOptionsResponse(Set<ForecastModel.ModelType> allowedTypes) {
}
