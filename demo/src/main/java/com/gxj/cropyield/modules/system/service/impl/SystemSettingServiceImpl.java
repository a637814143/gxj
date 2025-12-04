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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
/**
 * 系统设置模块的业务实现类，负责落实系统设置领域的业务处理逻辑。
 * <p>核心方法：getCurrentSettings、toResponse、updateSettings、createDefaultSetting、normalizeNullable。</p>
 */

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final RegionRepository regionRepository;
    private final TransactionTemplate transactionTemplate;

    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository,
                                    RegionRepository regionRepository,
                                    PlatformTransactionManager transactionManager) {
        this.systemSettingRepository = systemSettingRepository;
        this.regionRepository = regionRepository;
        this.transactionTemplate = createRequiresNewTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getCurrentSettings() {
        return systemSettingRepository.findTopByOrderByIdAsc()
            .map(this::toResponse)
            .orElseGet(() -> toResponse(createDefaultSettingInNewTransaction()));
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

        SystemSetting saved = systemSettingRepository.saveAndFlush(setting);
        return toResponse(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SystemSetting createDefaultSettingInNewTransaction() {
        return createDefaultSettingWithTemplate();
    }

    private SystemSetting createDefaultSetting() {
        SystemSetting setting = new SystemSetting();
        return systemSettingRepository.save(setting);
    }

    private SystemSetting createDefaultSettingWithTemplate() {
        return transactionTemplate.execute(status -> createDefaultSetting());
    }

    private TransactionTemplate createRequiresNewTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        return template;
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
