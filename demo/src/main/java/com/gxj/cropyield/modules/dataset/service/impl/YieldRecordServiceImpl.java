package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordDetailView;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordDetailView.CropInfo;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordDetailView.RegionInfo;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.dataset.service.YieldRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class YieldRecordServiceImpl implements YieldRecordService {

    private final YieldRecordRepository yieldRecordRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;

    public YieldRecordServiceImpl(YieldRecordRepository yieldRecordRepository,
                                  CropRepository cropRepository,
                                  RegionRepository regionRepository) {
        this.yieldRecordRepository = yieldRecordRepository;
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<YieldRecordDetailView> listAll() {
        return yieldRecordRepository.findAllWithRelations().stream()
                .sorted(Comparator.comparing(YieldRecord::getYear, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(record -> {
                            Region region = record.getRegion();
                            return region != null ? region.getName() : null;
                        }, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToView)
                .toList();
    }

    @Override
    @Transactional
    public YieldRecordDetailView create(YieldRecordRequest request) {
        Crop crop = cropRepository.findById(request.cropId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "作物不存在"));
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));

        YieldRecord record = new YieldRecord();
        record.setCrop(crop);
        record.setRegion(region);
        record.setYear(request.year());
        record.setSownArea(request.sownArea());
        record.setProduction(request.production());
        record.setYieldPerHectare(request.yieldPerHectare());
        record.setAveragePrice(request.averagePrice());
        record.setDataSource(request.dataSource());
        record.setCollectedAt(request.collectedAt());
        YieldRecord saved = yieldRecordRepository.save(record);
        return mapToView(saved);
    }

    private YieldRecordDetailView mapToView(YieldRecord record) {
        Crop crop = record.getCrop();
        Region region = record.getRegion();
        CropInfo cropInfo = crop == null ? null : new CropInfo(
                crop.getId(),
                crop.getCode(),
                crop.getName(),
                crop.getCategory(),
                crop.getDescription()
        );
        RegionInfo regionInfo = region == null ? null : new RegionInfo(
                region.getId(),
                region.getCode(),
                region.getName(),
                region.getLevel(),
                region.getParentCode(),
                region.getParentName(),
                region.getDescription()
        );

        return new YieldRecordDetailView(
                record.getId(),
                cropInfo,
                regionInfo,
                record.getYear(),
                record.getSownArea(),
                record.getProduction(),
                record.getYieldPerHectare(),
                record.getAveragePrice(),
                record.getDataSource(),
                record.getCollectedAt(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
