package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ModelRegistry;
import com.gxj.cropyield.modules.forecast.repository.ModelRegistryRepository;
import com.gxj.cropyield.modules.forecast.service.ModelRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelRegistryServiceImpl implements ModelRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistryServiceImpl.class);

    private final ModelRegistryRepository modelRegistryRepository;

    public ModelRegistryServiceImpl(ModelRegistryRepository modelRegistryRepository) {
        this.modelRegistryRepository = modelRegistryRepository;
    }

    @Override
    @Transactional
    public ModelRegistry registerSnapshot(ForecastModel model, String storageUri, String metricsJson) {
        if (model == null || storageUri == null) {
            log.warn("Skip registering model snapshot due to missing model or storage uri");
            return null;
        }
        ModelRegistry registry = new ModelRegistry();
        registry.setModelName(model.getName());
        registry.setModelType(model.getType());
        registry.setStorageUri(storageUri);
        registry.setMetricsJson(metricsJson);
        return modelRegistryRepository.save(registry);
    }
}
