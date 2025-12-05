package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastModelRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastModelAccessService;
import com.gxj.cropyield.modules.forecast.service.ForecastModelService;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.service.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预测管理模块的业务实现类，负责落实预测管理领域的业务处理逻辑。
 * <p>核心方法：listAll、create、update、duplicate、toggleEnabled。</p>
 */

@Service
public class ForecastModelServiceImpl implements ForecastModelService {

    private final ForecastModelRepository forecastModelRepository;
    private final ForecastModelAccessService forecastModelAccessService;
    private final CurrentUserService currentUserService;

    public ForecastModelServiceImpl(ForecastModelRepository forecastModelRepository,
                                    ForecastModelAccessService forecastModelAccessService,
                                    CurrentUserService currentUserService) {
        this.forecastModelRepository = forecastModelRepository;
        this.forecastModelAccessService = forecastModelAccessService;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<ForecastModel> listAll() {
        Set<ForecastModel.ModelType> allowedTypes = forecastModelAccessService.getEffectiveAllowedTypesForCurrentUser();
        return allowedTypes.isEmpty()
                ? List.of()
                : forecastModelRepository.findByTypeIn(List.copyOf(allowedTypes));
    }

    @Override
    public List<ForecastModel> listAvailable() {
        Set<ForecastModel.ModelType> allowedTypes = forecastModelAccessService.getEffectiveAllowedTypesForCurrentUser();
        if (allowedTypes.isEmpty()) {
            return List.of();
        }
        return forecastModelRepository.findByEnabledTrueAndTypeIn(List.copyOf(allowedTypes));
    }

    @Override
    @Transactional
    public ForecastModel create(ForecastModelRequest request) {
        ensureManagementPermission();
        validateType(request.type());
        ForecastModel model = new ForecastModel();
        applyRequest(model, request);
        return forecastModelRepository.save(model);
    }

    @Override
    @Transactional
    public ForecastModel update(Long id, ForecastModelRequest request) {
        ensureManagementPermission();
        ForecastModel model = forecastModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));
        validateType(request.type());
        applyRequest(model, request);
        return forecastModelRepository.save(model);
    }

    @Override
    @Transactional
    public ForecastModel duplicate(Long id) {
        ensureManagementPermission();
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
        ensureManagementPermission();
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
        if (type == null) {
            throw new IllegalArgumentException("模型类型不能为空");
        }
        if (!EnumSet.allOf(ForecastModel.ModelType.class).contains(type)) {
            throw new IllegalArgumentException("模型类型无效");
        }
        Set<ForecastModel.ModelType> allowedTypes = forecastModelAccessService.getEffectiveAllowedTypesForCurrentUser();
        if (!allowedTypes.contains(type)) {
            throw new IllegalArgumentException("模型类型不在允许范围内");
        }
    }

    private void ensureManagementPermission() {
        User user = currentUserService.getCurrentUser();
        if (!forecastModelAccessService.canUserManageModels(user)) {
            throw new IllegalArgumentException("当前账号未被授权管理模型");
        }
    }
}
