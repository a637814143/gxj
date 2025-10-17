package com.gxj.cropyield.datamanagement.model;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_import_job", indexes = {
        @Index(name = "idx_data_import_job_task", columnList = "task_id", unique = true),
        @Index(name = "idx_data_import_job_status", columnList = "status")
})
public class DataImportJob extends BaseEntity {

    @Column(name = "task_id", nullable = false, length = 64, unique = true)
    private String taskId;

    @Column(name = "dataset_name", length = 128)
    private String datasetName;

    @Column(name = "dataset_description", length = 256)
    private String datasetDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset_type", nullable = false, length = 32)
    private DatasetType datasetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DataImportJobStatus status = DataImportJobStatus.QUEUED;

    @Column(name = "original_filename", length = 256)
    private String originalFilename;

    @Column(name = "storage_path", length = 512)
    private String storagePath;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "processed_rows")
    private Integer processedRows;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;

    @Column(name = "skipped_rows")
    private Integer skippedRows;

    @Column(name = "failed_rows")
    private Integer failedRows;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(length = 512)
    private String message;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Lob
    @Column(name = "warnings_payload")
    private String warningsPayload;

    @Lob
    @Column(name = "preview_payload")
    private String previewPayload;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DataImportJobError> errors = new ArrayList<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetDescription() {
        return datasetDescription;
    }

    public void setDatasetDescription(String datasetDescription) {
        this.datasetDescription = datasetDescription;
    }

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DatasetType datasetType) {
        this.datasetType = datasetType;
    }

    public DataImportJobStatus getStatus() {
        return status;
    }

    public void setStatus(DataImportJobStatus status) {
        this.status = status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(Integer processedRows) {
        this.processedRows = processedRows;
    }

    public Integer getInsertedRows() {
        return insertedRows;
    }

    public void setInsertedRows(Integer insertedRows) {
        this.insertedRows = insertedRows;
    }

    public Integer getUpdatedRows() {
        return updatedRows;
    }

    public void setUpdatedRows(Integer updatedRows) {
        this.updatedRows = updatedRows;
    }

    public Integer getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(Integer skippedRows) {
        this.skippedRows = skippedRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getWarningsPayload() {
        return warningsPayload;
    }

    public void setWarningsPayload(String warningsPayload) {
        this.warningsPayload = warningsPayload;
    }

    public String getPreviewPayload() {
        return previewPayload;
    }

    public void setPreviewPayload(String previewPayload) {
        this.previewPayload = previewPayload;
    }

    public List<DataImportJobError> getErrors() {
        return errors;
    }

    public void setErrors(List<DataImportJobError> errors) {
        this.errors = errors;
    }

    public void addError(DataImportJobError error) {
        this.errors.add(error);
        error.setJob(this);
    }

    public void clearErrors() {
        for (DataImportJobError error : errors) {
            error.setJob(null);
        }
        this.errors.clear();
    }
}
