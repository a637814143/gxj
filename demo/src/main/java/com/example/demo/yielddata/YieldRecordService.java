package com.example.demo.yielddata;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YieldRecordService {

    private final YieldRecordRepository yieldRecordRepository;

    public YieldRecordService(YieldRecordRepository yieldRecordRepository) {
        this.yieldRecordRepository = yieldRecordRepository;
    }

    public List<YieldRecordResponse> search(Long cropId, Long regionId, Integer startYear, Integer endYear) {
        return yieldRecordRepository.search(cropId, regionId, startYear, endYear).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private YieldRecordResponse mapToResponse(YieldRecord record) {
        double revenue = 0.0;
        if (record.getProduction() != null && record.getAveragePrice() != null) {
            revenue = record.getProduction() * record.getAveragePrice() * 0.1;
        }
        return new YieldRecordResponse(
                record.getId(),
                record.getCrop().getName(),
                record.getCrop().getCategory(),
                record.getRegion().getName(),
                record.getRegion().getLevel(),
                record.getYear(),
                record.getSownArea(),
                record.getProduction(),
                record.getYieldPerHectare(),
                record.getAveragePrice(),
                revenue,
                record.getDataSource(),
                record.getCollectedAt()
        );
    }
}
