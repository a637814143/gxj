package com.gxj.cropyield.datamanagement.model;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
/**
 * 数据导入模块的模型定义，描述数据导入领域的公共数据结构。
 */

@Entity
@Table(name = "data_import_job_error")
public class DataImportJobError extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private DataImportJob job;

    @Column(name = "line_number")
    private Integer rowNumber;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(length = 512)
    private String message;

    @Column(name = "raw_value", length = 512)
    private String rawValue;

    public DataImportJob getJob() {
        return job;
    }

    public void setJob(DataImportJob job) {
        this.job = job;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }
}
