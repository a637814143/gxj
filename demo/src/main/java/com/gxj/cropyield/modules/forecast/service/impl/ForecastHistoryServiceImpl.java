package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryPageResponse;
import com.gxj.cropyield.modules.forecast.dto.ForecastHistoryResponse;
import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import com.gxj.cropyield.modules.forecast.entity.ForecastRun;
import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastSnapshotRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ForecastHistoryServiceImpl implements ForecastHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ForecastHistoryServiceImpl.class);

    private final ForecastSnapshotRepository forecastSnapshotRepository;
    private final ForecastRunRepository forecastRunRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastResultRepository forecastResultRepository;

    public ForecastHistoryServiceImpl(ForecastSnapshotRepository forecastSnapshotRepository,
                                      ForecastRunRepository forecastRunRepository,
                                      ForecastTaskRepository forecastTaskRepository,
                                      ForecastResultRepository forecastResultRepository) {
        this.forecastSnapshotRepository = forecastSnapshotRepository;
        this.forecastRunRepository = forecastRunRepository;
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastResultRepository = forecastResultRepository;
    }

    @Override
    public ForecastHistoryPageResponse getHistory(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeSize, sort);
        try {
            Page<ForecastSnapshot> snapshotPage = forecastSnapshotRepository
                .findAllByOrderByCreatedAtDesc(pageRequest);

            if (snapshotPage.getTotalPages() > 0 && pageRequest.getPageNumber() >= snapshotPage.getTotalPages()) {
                pageRequest = PageRequest.of(snapshotPage.getTotalPages() - 1, pageRequest.getPageSize(), sort);
                snapshotPage = forecastSnapshotRepository.findAllByOrderByCreatedAtDesc(pageRequest);
            }

            List<ForecastHistoryResponse> items = snapshotPage
                .stream()
                .map(this::mapSnapshot)
                .toList();

            int currentPage = snapshotPage.getTotalPages() == 0
                ? 1
                : pageRequest.getPageNumber() + 1;

            return new ForecastHistoryPageResponse(
                items,
                snapshotPage.getTotalElements(),
                currentPage,
                pageRequest.getPageSize()
            );
        } catch (DataAccessException ex) {
            log.warn("Failed to load forecast history snapshots, returning empty list", ex);
            return new ForecastHistoryPageResponse(List.of(), 0L, 1, safeSize);
        }
    }

    @Override
    @Transactional
    public void deleteHistory(Long runId) {
        if (runId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预测记录编号不能为空");
        }
        try {
            ForecastRun run = forecastRunRepository.findById(runId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "预测记录不存在"));
            List<ForecastSnapshot> snapshots = forecastSnapshotRepository.findByRunIdOrderByPeriodAsc(runId);
            removeForecastResults(run, snapshots);
            deleteSnapshots(snapshots);
            forecastRunRepository.delete(run);
        } catch (BusinessException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            log.error("Failed to delete forecast run {}", runId, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "删除预测记录失败");
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

    private void removeForecastResults(ForecastRun run, List<ForecastSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        forecastTaskRepository.findByModelIdAndCropIdAndRegionId(
                run.getModel().getId(),
                run.getCrop().getId(),
                run.getRegion().getId()
            )
            .ifPresent(task -> snapshots.stream()
                .map(ForecastSnapshot::getYear)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(year -> {
                    try {
                        forecastResultRepository.findByTaskIdAndTargetYear(task.getId(), year)
                            .ifPresent(forecastResultRepository::delete);
                    } catch (DataAccessException ex) {
                        log.warn("Failed to delete forecast result for task {} and year {}", task.getId(), year, ex);
                    }
                }));
    }

    private void deleteSnapshots(List<ForecastSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        forecastSnapshotRepository.deleteAll(snapshots);
        forecastSnapshotRepository.flush();
    }
}
