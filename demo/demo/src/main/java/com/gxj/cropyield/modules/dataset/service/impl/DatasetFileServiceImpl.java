package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.datamanagement.model.DataImportJob;
import com.gxj.cropyield.datamanagement.model.DataImportJobStatus;
import com.gxj.cropyield.datamanagement.repository.DataImportJobRepository;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import com.gxj.cropyield.modules.dataset.repository.DatasetFileRepository;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import com.gxj.cropyield.modules.forecast.repository.ForecastRunRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * 数据集管理模块的业务实现类，负责落实数据集管理领域的业务处理逻辑。
 * <p>核心方法：listAll、create、deleteByIds、archiveImportJobs、cleanupDatasetRecords、recordDeletionJob、collectCleanupImpact、pruneBaseEntities。</p>
 */

@Service
public class DatasetFileServiceImpl implements DatasetFileService {

    private final DatasetFileRepository datasetFileRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final DataImportJobRepository jobRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;
    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastRunRepository forecastRunRepository;

    public DatasetFileServiceImpl(DatasetFileRepository datasetFileRepository,
                                  YieldRecordRepository yieldRecordRepository,
                                  DataImportJobRepository jobRepository,
                                  CropRepository cropRepository,
                                  RegionRepository regionRepository,
                                  ForecastTaskRepository forecastTaskRepository,
                                  ForecastRunRepository forecastRunRepository) {
        this.datasetFileRepository = datasetFileRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.jobRepository = jobRepository;
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastRunRepository = forecastRunRepository;
    }

    @Override
    public List<DatasetFile> listAll() {
        return datasetFileRepository.findAll();
    }

    @Override
    public DatasetFile create(DatasetFileRequest request) {
        DatasetFile datasetFile = new DatasetFile();
        datasetFile.setName(request.name());
        datasetFile.setType(request.type());
        datasetFile.setStoragePath(request.storagePath());
        datasetFile.setDescription(request.description());
        return datasetFileRepository.save(datasetFile);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<DatasetFile> files = datasetFileRepository.findAllById(ids);
        if (files.isEmpty()) {
            return;
        }
        Set<Long> candidateCropIds = new LinkedHashSet<>();
        Set<Long> candidateRegionIds = new LinkedHashSet<>();
        for (DatasetFile file : files) {
            archiveImportJobs(file);
            CleanupImpact impact = collectCleanupImpact(file);
            candidateCropIds.addAll(impact.cropIds());
            candidateRegionIds.addAll(impact.regionIds());
            long removed = cleanupDatasetRecords(file);
            recordDeletionJob(file, removed);
        }
        datasetFileRepository.deleteAll(files);
        pruneBaseEntities(candidateCropIds, candidateRegionIds);
    }

    private void archiveImportJobs(DatasetFile file) {
        Set<DataImportJob> relatedJobs = new LinkedHashSet<>();
        if (file.getId() != null) {
            relatedJobs.addAll(jobRepository.findByDatasetFileId(file.getId()));
        }
        if (file.getName() != null && !file.getName().isBlank()) {
            relatedJobs.addAll(jobRepository.findByDatasetName(file.getName()));
        }
        if (relatedJobs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        relatedJobs.forEach(job -> {
            job.setDatasetFileId(null);
            if (job.getFinishedAt() == null) {
                job.setFinishedAt(now);
            }
            String original = job.getMessage();
            if (original == null || original.isBlank()) {
                job.setMessage("关联数据集已删除，记录已归档。");
            } else if (!original.contains("关联数据集已删除")) {
                job.setMessage("关联数据集已删除，记录已归档。原摘要：" + original);
            }
        });
        jobRepository.saveAll(relatedJobs);
    }

    private long cleanupDatasetRecords(DatasetFile file) {
        DatasetType type = file.getType();
        if (type == null || file.getId() == null) {
            return 0L;
        }
        Long removed = switch (type) {
            case YIELD -> yieldRecordRepository.deleteByDatasetFileId(file.getId());
            default -> 0L;
        };
        return removed == null ? 0L : removed;
    }

    private void recordDeletionJob(DatasetFile file, long removedRecords) {
        DataImportJob job = new DataImportJob();
        job.setTaskId("DELETE-" + UUID.randomUUID());
        job.setDatasetName(file.getName());
        job.setDatasetDescription(file.getDescription());
        job.setDatasetType(file.getType());
        job.setStatus(DataImportJobStatus.SUCCEEDED);
        job.setDatasetFileId(null);
        job.setOriginalFilename(file.getName());
        job.setStoragePath(file.getStoragePath());
        int processed = removedRecords > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) removedRecords;
        job.setTotalRows(processed);
        job.setProcessedRows(processed);
        job.setInsertedRows(0);
        job.setUpdatedRows(0);
        job.setSkippedRows(0);
        job.setFailedRows(0);
        job.setWarningCount(0);
        job.setMessage("数据集已删除，清理 " + removedRecords + " 条入库记录");
        LocalDateTime now = LocalDateTime.now();
        job.setStartedAt(now);
        job.setFinishedAt(now);
        jobRepository.save(job);
    }

    private CleanupImpact collectCleanupImpact(DatasetFile file) {
        if (file.getId() == null) {
            return new CleanupImpact(Set.of(), Set.of());
        }
        Set<Long> cropIds = new LinkedHashSet<>();
        Set<Long> regionIds = new LinkedHashSet<>();
        cropIds.addAll(yieldRecordRepository.findDistinctCropIdsByDatasetFileId(file.getId()));
        regionIds.addAll(yieldRecordRepository.findDistinctRegionIdsByDatasetFileId(file.getId()));
        return new CleanupImpact(cropIds, regionIds);
    }

    private void pruneBaseEntities(Set<Long> cropIds, Set<Long> regionIds) {
        if (!cropIds.isEmpty()) {
            List<Long> removableCrops = cropIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> !yieldRecordRepository.existsByCropId(id))
                    .filter(id -> !forecastTaskRepository.existsByCropId(id))
                    .filter(id -> !forecastRunRepository.existsByCropId(id))
                    .collect(Collectors.toList());
            if (!removableCrops.isEmpty()) {
                cropRepository.deleteAllById(removableCrops);
            }
        }
        if (!regionIds.isEmpty()) {
            List<Long> removableRegions = regionIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> !yieldRecordRepository.existsByRegionId(id))
                    .filter(id -> !forecastTaskRepository.existsByRegionId(id))
                    .filter(id -> !forecastRunRepository.existsByRegionId(id))
                    .collect(Collectors.toList());
            if (!removableRegions.isEmpty()) {
                regionRepository.deleteAllById(removableRegions);
            }
        }
    }

    private record CleanupImpact(Set<Long> cropIds, Set<Long> regionIds) {
    }
}
