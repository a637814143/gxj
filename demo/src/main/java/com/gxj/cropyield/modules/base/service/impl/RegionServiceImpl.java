package com.gxj.cropyield.modules.base.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.base.service.RegionService;
import com.gxj.cropyield.modules.dataset.repository.PriceRecordRepository;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastRunRepository forecastRunRepository;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9-]");

    public RegionServiceImpl(RegionRepository regionRepository,
                             YieldRecordRepository yieldRecordRepository,
                             PriceRecordRepository priceRecordRepository,
                             ForecastTaskRepository forecastTaskRepository,
                             ForecastRunRepository forecastRunRepository) {
        this.regionRepository = regionRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.priceRecordRepository = priceRecordRepository;
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastRunRepository = forecastRunRepository;
    }

    @Override
    public List<Region> listAll() {
        return regionRepository.findAll();
    }

    @Override
    @Transactional
    public Region create(RegionRequest request) {
        Region region = new Region();
        applyRequest(region, request, true);
        return regionRepository.save(region);
    }

    @Override
    @Transactional
    public Region update(Long id, RegionRequest request) {
        Region region = regionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));
        applyRequest(region, request, false);
        return regionRepository.save(region);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Region region = regionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));

        boolean inUse = yieldRecordRepository.existsByRegionId(id)
            || priceRecordRepository.existsByRegionId(id)
            || forecastTaskRepository.existsByRegionId(id)
            || forecastRunRepository.existsByRegionId(id);
        if (inUse) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "区域已被数据或任务引用，无法删除");
        }

        regionRepository.delete(region);
    }

    private void applyRequest(Region region, RegionRequest request, boolean isCreate) {
        if (!StringUtils.hasText(request.name())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "区域名称不能为空");
        }

        String normalizedName = request.name().trim();
        ensureNameUnique(normalizedName, region.getId());
        region.setName(normalizedName);

        String resolvedCode = resolveCode(request.code(), normalizedName, region.getId(), isCreate);
        region.setCode(resolvedCode);

        String level = resolveLevel(request.level(), region.getLevel(), isCreate);
        region.setLevel(level);

        region.setParentCode(normalizeNullable(request.parentCode()));
        region.setParentName(normalizeNullable(request.parentName()));
        region.setDescription(normalizeNullable(request.description()));
    }

    private void ensureNameUnique(String name, Long currentId) {
        regionRepository.findByNameIgnoreCase(name)
            .filter(existing -> !Objects.equals(existing.getId(), currentId))
            .ifPresent(existing -> {
                throw new BusinessException(ResultCode.BAD_REQUEST, "区域名称已存在");
            });
    }

    private String resolveCode(String requestedCode, String name, Long currentId, boolean isCreate) {
        String candidate = StringUtils.hasText(requestedCode)
            ? requestedCode.trim()
            : (isCreate ? generateCodeFromName(name) : null);

        if (!StringUtils.hasText(candidate)) {
            candidate = currentId != null
                ? regionRepository.findById(currentId).map(Region::getCode).orElseGet(() -> generateCodeFromName(name))
                : generateCodeFromName(name);
        }

        String normalized = normalizeCode(candidate);
        return ensureUniqueCode(normalized, currentId);
    }

    private String normalizeCode(String raw) {
        String upper = raw.toUpperCase(Locale.ROOT);
        String sanitized = NON_ALPHANUMERIC.matcher(upper).replaceAll("-");
        sanitized = sanitized.replaceAll("-+", "-");
        sanitized = sanitized.replaceAll("^-|-$", "");
        return sanitized.isEmpty() ? "REGION" : sanitized;
    }

    private String ensureUniqueCode(String code, Long currentId) {
        String candidate = code;
        int suffix = 1;
        while (true) {
            Optional<Region> existing = regionRepository.findByCode(candidate);
            if (existing.isEmpty() || Objects.equals(existing.get().getId(), currentId)) {
                return candidate;
            }
            candidate = String.format(Locale.ROOT, "%s-%d", code, suffix++);
        }
    }

    private String generateCodeFromName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(normalized)) {
            String hash = Integer.toUnsignedString(Math.abs(name.hashCode()), 36);
            normalized = "REG-" + hash;
        }
        return normalizeCode(normalized);
    }

    private String resolveLevel(String requestedLevel, String currentLevel, boolean isCreate) {
        if (StringUtils.hasText(requestedLevel)) {
            return requestedLevel.trim().toUpperCase(Locale.ROOT);
        }
        if (isCreate && !StringUtils.hasText(currentLevel)) {
            return "PREFECTURE";
        }
        return currentLevel;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
