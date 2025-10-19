package com.gxj.cropyield.modules.system.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.system.dto.SystemSettingRequest;
import com.gxj.cropyield.modules.system.dto.SystemSettingResponse;
import com.gxj.cropyield.modules.system.entity.SystemSetting;
import com.gxj.cropyield.modules.system.repository.SystemSettingRepository;
import com.gxj.cropyield.modules.system.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final RegionRepository regionRepository;

    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository,
                                    RegionRepository regionRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getCurrentSettings() {
        SystemSetting setting = systemSettingRepository.findTopByOrderByIdAsc()
            .orElseGet(this::createDefaultSetting);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SystemSettingResponse updateSettings(SystemSettingRequest request) {
        SystemSetting setting = systemSettingRepository.findTopByOrderByIdAsc()
            .orElseGet(this::createDefaultSetting);

        if (request.defaultRegionId() != null) {
            Region region = regionRepository.findById(request.defaultRegionId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "默认区域不存在"));
            setting.setDefaultRegion(region);
        } else {
            setting.setDefaultRegion(null);
        }

        setting.setNotifyEmail(normalizeNullable(request.notifyEmail()));
        if (request.clusterEnabled() != null) {
            setting.setClusterEnabled(request.clusterEnabled());
        }
        if (request.pendingChangeCount() != null) {
            setting.setPendingChangeCount(request.pendingChangeCount());
        }
        setting.setSecurityStrategy(normalizeNullable(request.securityStrategy()));

        SystemSetting saved = systemSettingRepository.save(setting);
        return toResponse(saved);
    }

    private SystemSetting createDefaultSetting() {
        SystemSetting setting = new SystemSetting();
        return systemSettingRepository.save(setting);
    }

    private SystemSettingResponse toResponse(SystemSetting setting) {
        Region defaultRegion = setting.getDefaultRegion();
        return new SystemSettingResponse(
            setting.getId(),
            defaultRegion != null ? defaultRegion.getId() : null,
            defaultRegion != null ? defaultRegion.getName() : null,
            setting.getNotifyEmail(),
            setting.isClusterEnabled(),
            setting.getPendingChangeCount(),
            setting.getSecurityStrategy(),
            setting.getUpdatedAt()
        );
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
