package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 记录模型产出的元数据与模型文件落盘位置，便于复现与版本管理。
 */
@Entity
@Table(name = "model_registry")
public class ModelRegistry extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ForecastModel.ModelType modelType;

    @Column(nullable = false, length = 255)
    private String storageUri;

    @Column(length = 1024)
    private String metricsJson;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public ForecastModel.ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ForecastModel.ModelType modelType) {
        this.modelType = modelType;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public void setMetricsJson(String metricsJson) {
        this.metricsJson = metricsJson;
    }
}
