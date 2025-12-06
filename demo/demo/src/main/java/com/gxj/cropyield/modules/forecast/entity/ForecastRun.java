package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
/**
 * 预测管理模块的实体类，映射预测管理领域对应的数据表结构。
 */

@Entity
@Table(name = "forecast_run")
public class ForecastRun extends BaseEntity {

    public enum RunStatus {
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
    private RunStatus status = RunStatus.PENDING;

    @Column(nullable = false)
    private Integer forecastPeriods;

    @Column
    private Integer historyYears;

    @Column(length = 32)
    private String frequency;

    @Column(length = 64)
    private String externalRequestId;

    @Column(length = 512)
    private String errorMessage;

    @Column
    private Double mae;

    @Column
    private Double rmse;

    @Column
    private Double mape;

    @Column
    private Double r2;

    @Column(length = 64)
    private String measurementLabel;

    @Column(length = 32)
    private String measurementUnit;

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

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Integer getForecastPeriods() {
        return forecastPeriods;
    }

    public void setForecastPeriods(Integer forecastPeriods) {
        this.forecastPeriods = forecastPeriods;
    }

    public Integer getHistoryYears() {
        return historyYears;
    }

    public void setHistoryYears(Integer historyYears) {
        this.historyYears = historyYears;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getExternalRequestId() {
        return externalRequestId;
    }

    public void setExternalRequestId(String externalRequestId) {
        this.externalRequestId = externalRequestId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Double getMae() {
        return mae;
    }

    public void setMae(Double mae) {
        this.mae = mae;
    }

    public Double getRmse() {
        return rmse;
    }

    public void setRmse(Double rmse) {
        this.rmse = rmse;
    }

    public Double getMape() {
        return mape;
    }

    public void setMape(Double mape) {
        this.mape = mape;
    }

    public Double getR2() {
        return r2;
    }

    public void setR2(Double r2) {
        this.r2 = r2;
    }

    public String getMeasurementLabel() {
        return measurementLabel;
    }

    public void setMeasurementLabel(String measurementLabel) {
        this.measurementLabel = measurementLabel;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }
}
