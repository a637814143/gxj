package com.dali.cropyield.modules.dataset.service.impl;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.entity.Region;
import com.dali.cropyield.modules.base.repository.CropRepository;
import com.dali.cropyield.modules.base.repository.RegionRepository;
import com.dali.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.dali.cropyield.modules.dataset.entity.YieldRecord;
import com.dali.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.dali.cropyield.modules.dataset.service.YieldRecordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YieldRecordServiceImpl implements YieldRecordService {

    private final YieldRecordRepository yieldRecordRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;

    @Override
    public List<YieldRecord> findAll() {
        return yieldRecordRepository.findAll();
    }

    @Override
    @Transactional
    public YieldRecord create(YieldRecordRequest request) {
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> BusinessException.notFound("作物不存在"));
        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> BusinessException.notFound("地区不存在"));

        YieldRecord record = new YieldRecord();
        record.setCrop(crop);
        record.setRegion(region);
        record.setYear(request.year());
        record.setSownArea(request.sownArea());
        record.setHarvestedArea(request.harvestedArea());
        record.setProduction(request.production());
        record.setYieldPerMu(request.yieldPerMu());
        record.setDataSource(request.dataSource());
        record.setDataVersion(request.dataVersion());
        return yieldRecordRepository.save(record);
    }
}
