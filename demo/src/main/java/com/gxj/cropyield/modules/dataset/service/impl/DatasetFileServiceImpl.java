package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.datamanagement.model.DataImportJob;
import com.gxj.cropyield.datamanagement.model.DataImportJobStatus;
import com.gxj.cropyield.datamanagement.repository.DataImportJobRepository;
import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import com.gxj.cropyield.modules.dataset.repository.DatasetFileRepository;
import com.gxj.cropyield.modules.dataset.repository.PriceRecordRepository;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DatasetFileServiceImpl implements DatasetFileService {

    private final DatasetFileRepository datasetFileRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final DataImportJobRepository jobRepository;

    public DatasetFileServiceImpl(DatasetFileRepository datasetFileRepository,
                                  YieldRecordRepository yieldRecordRepository,
                                  PriceRecordRepository priceRecordRepository,
                                  DataImportJobRepository jobRepository) {
        this.datasetFileRepository = datasetFileRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.priceRecordRepository = priceRecordRepository;
        this.jobRepository = jobRepository;
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
        for (DatasetFile file : files) {
            long removed = cleanupDatasetRecords(file);
            recordDeletionJob(file, removed);
        }
        datasetFileRepository.deleteAll(files);
    }

    private long cleanupDatasetRecords(DatasetFile file) {
        DatasetType type = file.getType();
        if (type == null || file.getId() == null) {
            return 0L;
        }
        return switch (type) {
            case YIELD -> yieldRecordRepository.deleteByDatasetFileId(file.getId());
            case PRICE -> priceRecordRepository.deleteByDatasetFileId(file.getId());
            default -> 0L;
        };
    }

    private void recordDeletionJob(DatasetFile file, long removedRecords) {
        DataImportJob job = new DataImportJob();
        job.setTaskId("DELETE-" + UUID.randomUUID());
        job.setDatasetName(file.getName());
        job.setDatasetDescription(file.getDescription());
        job.setDatasetType(file.getType());
        job.setStatus(DataImportJobStatus.SUCCEEDED);
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
}
