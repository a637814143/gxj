package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.config.ForecastModelSettings;
import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import jakarta.persistence.EntityNotFoundException;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预测管理模块的业务实现类，负责落实预测管理领域的业务处理逻辑。
 * <p>核心方法：listAll、create、update、duplicate、toggleEnabled。</p>
 */

@Service
public class ForecastModelServiceImpl implements ForecastModelService {

    private final ForecastModelRepository forecastModelRepository;
    private final ForecastModelSettings forecastModelSettings;

    public ForecastModelServiceImpl(ForecastModelRepository forecastModelRepository,
                                    ForecastModelSettings forecastModelSettings) {
        this.forecastModelRepository = forecastModelRepository;
        this.forecastModelSettings = forecastModelSettings;
    }

    @Override
    public List<ForecastModel> listAll() {
        return forecastModelRepository.findAll();
    }

    @Override
    public List<ForecastModel> listAvailable() {
        List<ForecastModel.ModelType> allowedTypes = List.copyOf(forecastModelSettings.getAllowedTypes());
        return forecastModelRepository.findByEnabledTrueAndTypeIn(allowedTypes);
    }

    @Override
    @Transactional
    public ForecastModel create(ForecastModelRequest request) {
        validateType(request.type());
        ForecastModel model = new ForecastModel();
        applyRequest(model, request);
        return forecastModelRepository.save(model);
    }

    @Override
    @Transactional
    public ForecastModel update(Long id, ForecastModelRequest request) {
        ForecastModel model = forecastModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));
        validateType(request.type());
        applyRequest(model, request);
        return forecastModelRepository.save(model);
    }

    @Override
    @Transactional
    public ForecastModel duplicate(Long id) {
        ForecastModel source = forecastModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));
        ForecastModel copy = new ForecastModel();
        copy.setName(source.getName() + " 副本");
        copy.setType(source.getType());
        copy.setDescription(source.getDescription());
        copy.setCropScope(source.getCropScope());
        copy.setRegionScope(source.getRegionScope());
        copy.setGranularity(source.getGranularity());
        copy.setHistoryWindow(source.getHistoryWindow());
        copy.setForecastHorizon(source.getForecastHorizon());
        copy.setHyperParameters(source.getHyperParameters());
        copy.setEnabled(Boolean.TRUE.equals(source.getEnabled()));
        return forecastModelRepository.save(copy);
    }

    @Override
    @Transactional
    public ForecastModel toggleEnabled(Long id, boolean enabled) {
        ForecastModel model = forecastModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));
        model.setEnabled(enabled);
        return forecastModelRepository.save(model);
    }

    private void applyRequest(ForecastModel model, ForecastModelRequest request) {
        model.setName(request.name());
        model.setType(request.type());
        model.setDescription(request.description());
        model.setCropScope(request.cropScope());
        model.setRegionScope(request.regionScope());
        model.setGranularity(request.granularity());
        model.setHistoryWindow(request.historyWindow());
        model.setForecastHorizon(request.forecastHorizon());
        model.setHyperParameters(request.hyperParameters());
        model.setEnabled(request.enabled());
    }

    private void validateType(ForecastModel.ModelType type) {
        if (!forecastModelSettings.isTypeAllowed(type)) {
            throw new IllegalArgumentException("模型类型不在允许范围内");
        }
        if (type == null) {
            throw new IllegalArgumentException("模型类型不能为空");
        }
        if (!EnumSet.allOf(ForecastModel.ModelType.class).contains(type)) {
            throw new IllegalArgumentException("模型类型无效");
        }
    }
}
