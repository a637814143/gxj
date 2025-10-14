package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.logging.AuditLogger;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastModelServiceImpl implements ForecastModelService {

    private final ForecastModelRepository forecastModelRepository;
    private final AuditLogger auditLogger;

    public ForecastModelServiceImpl(ForecastModelRepository forecastModelRepository, AuditLogger auditLogger) {
        this.forecastModelRepository = forecastModelRepository;
        this.auditLogger = auditLogger;
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
        ForecastModel saved = forecastModelRepository.save(model);
        auditLogger.record("高级预测模型配置", "创建预测模型:" + saved.getName());
        return saved;
    }
}
