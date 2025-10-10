package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.PriceRecord;
import com.gxj.cropyield.modules.dataset.repository.PriceRecordRepository;
import com.gxj.cropyield.modules.dataset.service.PriceRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PriceRecordServiceImpl implements PriceRecordService {

    private final PriceRecordRepository priceRecordRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;

    public PriceRecordServiceImpl(PriceRecordRepository priceRecordRepository,
                                  CropRepository cropRepository,
                                  RegionRepository regionRepository) {
        this.priceRecordRepository = priceRecordRepository;
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public List<PriceRecord> listAll() {
        return priceRecordRepository.findAll();
    }

    @Override
    @Transactional
    public PriceRecord create(PriceRecordRequest request) {
        Crop crop = cropRepository.findById(request.cropId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "作物不存在"));
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));

        PriceRecord record = new PriceRecord();
        record.setCrop(crop);
        record.setRegion(region);
        record.setRecordDate(request.recordDate());
        record.setPrice(request.price());
        return priceRecordRepository.save(record);
    }
}
