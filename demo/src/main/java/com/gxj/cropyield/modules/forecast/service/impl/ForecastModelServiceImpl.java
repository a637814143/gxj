package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastModelServiceImpl implements ForecastModelService {

    private final ForecastModelRepository forecastModelRepository;

    public ForecastModelServiceImpl(ForecastModelRepository forecastModelRepository) {
        this.forecastModelRepository = forecastModelRepository;
    }

    @Override
    public List<ForecastModel> listAll() {
        return forecastModelRepository.findAll();
    }

    @Override
    public ForecastModel create(ForecastModelRequest request) {
        ForecastModel model = new ForecastModel();
        model.setName(request.name());
        model.setType(request.type());
        model.setDescription(request.description());
        return forecastModelRepository.save(model);
    }
}
