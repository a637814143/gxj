package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import com.gxj.cropyield.modules.forecast.repository.ForecastSnapshotRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastHistoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ForecastHistoryServiceImpl implements ForecastHistoryService {

    private final ForecastSnapshotRepository forecastSnapshotRepository;

    public ForecastHistoryServiceImpl(ForecastSnapshotRepository forecastSnapshotRepository) {
        this.forecastSnapshotRepository = forecastSnapshotRepository;
    }

    @Override
    public List<ForecastHistoryResponse> getRecentHistory(int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 20);
        return forecastSnapshotRepository
            .findByOrderByCreatedAtDesc(PageRequest.of(0, effectiveLimit))
            .stream()
            .map(this::mapSnapshot)
            .toList();
    }

    private ForecastHistoryResponse mapSnapshot(ForecastSnapshot snapshot) {
        ForecastRun run = snapshot.getRun();
        LocalDateTime generatedAt = run.getUpdatedAt();
        return new ForecastHistoryResponse(
            run.getId(),
            snapshot.getPeriod(),
            snapshot.getYear(),
            run.getRegion().getName(),
            run.getCrop().getName(),
            run.getModel().getName(),
            run.getModel().getType().name(),
            snapshot.getMeasurementLabel(),
            snapshot.getMeasurementUnit(),
            snapshot.getMeasurementValue(),
            snapshot.getPredictedProduction(),
            snapshot.getPredictedYield(),
            snapshot.getSownArea(),
            snapshot.getAveragePrice(),
            snapshot.getEstimatedRevenue(),
            generatedAt
        );
    }
}
