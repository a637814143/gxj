package com.dali.cropyield.modules.forecast.service.impl;

import com.dali.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastModel;
import com.dali.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.dali.cropyield.modules.forecast.service.ForecastModelService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForecastModelServiceImpl implements ForecastModelService {

    private final ForecastModelRepository forecastModelRepository;

    @Override
    public List<ForecastModel> findAll() {
        return forecastModelRepository.findAll();
    }

    @Override
    @Transactional
    public ForecastModel create(ForecastModelRequest request) {
        ForecastModel model = new ForecastModel();
        model.setName(request.name());
        model.setAlgorithm(request.algorithm());
        model.setDescription(request.description());
        model.setHyperParameters(request.hyperParameters());
        return forecastModelRepository.save(model);
    }
}
