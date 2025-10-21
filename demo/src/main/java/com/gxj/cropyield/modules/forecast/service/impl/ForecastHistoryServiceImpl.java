package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastSnapshotRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ForecastHistoryServiceImpl implements ForecastHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ForecastHistoryServiceImpl.class);

    private final ForecastSnapshotRepository forecastSnapshotRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastResultRepository forecastResultRepository;

    public ForecastHistoryServiceImpl(ForecastSnapshotRepository forecastSnapshotRepository,
                                      ForecastTaskRepository forecastTaskRepository,
                                      ForecastResultRepository forecastResultRepository) {
        this.forecastSnapshotRepository = forecastSnapshotRepository;
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastResultRepository = forecastResultRepository;
    }

    @Override
    public List<ForecastHistoryResponse> getRecentHistory(int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 20);
        try {
            return forecastSnapshotRepository
                .findByOrderByCreatedAtDesc(PageRequest.of(0, effectiveLimit))
                .stream()
                .map(this::mapSnapshot)
                .toList();
        } catch (DataAccessException ex) {
            log.warn("Failed to load forecast history snapshots, returning empty list", ex);
            return Collections.emptyList();
        }
    }

    private ForecastHistoryResponse mapSnapshot(ForecastSnapshot snapshot) {
        ForecastRun run = snapshot.getRun();
        LocalDateTime generatedAt = run.getUpdatedAt();
        Long forecastResultId = resolveForecastResultId(run, snapshot);
        return new ForecastHistoryResponse(
            run.getId(),
            forecastResultId,
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

    private Long resolveForecastResultId(ForecastRun run, ForecastSnapshot snapshot) {
        Integer targetYear = snapshot.getYear();
        if (targetYear == null) {
            return null;
        }
        return forecastTaskRepository.findByModelIdAndCropIdAndRegionId(
                run.getModel().getId(),
                run.getCrop().getId(),
                run.getRegion().getId()
            )
            .flatMap(task -> forecastResultRepository.findByTaskIdAndTargetYear(task.getId(), targetYear))
            .map(ForecastResult::getId)
            .orElse(null);
    }
}
