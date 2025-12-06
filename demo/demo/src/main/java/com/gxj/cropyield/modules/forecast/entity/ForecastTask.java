package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import jakarta.persistence.*;
/**
 * 预测管理模块的实体类，映射预测管理领域对应的数据表结构。
 */

@Entity
@Table(name = "forecast_task")
public class ForecastTask extends BaseEntity {

    public enum TaskStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ForecastModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(length = 512)
    private String parameters;

    public ForecastModel getModel() {
        return model;
    }

    public void setModel(ForecastModel model) {
        this.model = model;
    }

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
