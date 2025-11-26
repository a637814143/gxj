package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.dataset.service.YieldRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 数据集管理模块的业务实现类，负责落实数据集管理领域的业务处理逻辑。
 * <p>核心方法：listAll、create。</p>
 */

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
    public List<YieldRecord> listAll() {
        return yieldRecordRepository.findAll();
    }

    @Override
    @Transactional
    public YieldRecord create(YieldRecordRequest request) {
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
        record.setDataSource(request.dataSource());
        record.setCollectedAt(request.collectedAt());
        return yieldRecordRepository.save(record);
    }
}
