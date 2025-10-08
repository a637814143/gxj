package com.dali.cropyield.modules.dataset.service.impl;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.repository.CropRepository;
import com.dali.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.dali.cropyield.modules.dataset.entity.PriceRecord;
import com.dali.cropyield.modules.dataset.repository.PriceRecordRepository;
import com.dali.cropyield.modules.dataset.service.PriceRecordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceRecordServiceImpl implements PriceRecordService {

    private final PriceRecordRepository priceRecordRepository;
    private final CropRepository cropRepository;

    @Override
    public List<PriceRecord> findAll() {
        return priceRecordRepository.findAll();
    }

    @Override
    @Transactional
    public PriceRecord create(PriceRecordRequest request) {
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> BusinessException.notFound("作物不存在"));
        PriceRecord record = new PriceRecord();
        record.setCrop(crop);
        record.setYear(request.year());
        record.setAveragePrice(request.averagePrice());
        record.setUnit(request.unit());
        record.setDataSource(request.dataSource());
        return priceRecordRepository.save(record);
    }
}
