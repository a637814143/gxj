package com.gxj.cropyield.datamanagement.dto;

import com.gxj.cropyield.datamanagement.model.DataImportJobStatus;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;

import java.time.LocalDateTime;
/**
 * 数据导入模块的数据传输对象（记录类型），在数据导入场景下承载参数与返回值。
 */

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
