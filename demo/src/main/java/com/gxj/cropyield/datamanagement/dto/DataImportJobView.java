package com.gxj.cropyield.datamanagement.dto;

import com.gxj.cropyield.datamanagement.model.DataImportJobStatus;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;

import java.time.LocalDateTime;

public record DataImportJobView(
        String taskId,
        Long datasetFileId,
        String datasetName,
        DatasetType datasetType,
        String originalFilename,
        DataImportJobStatus status,
        int totalRows,
        int processedRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int failedRows,
        int warningCount,
        double progress,
        String message,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
