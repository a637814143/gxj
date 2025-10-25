package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
/**
 * 预测管理模块的实体类，映射预测管理领域对应的数据表结构。
 */

@Entity
@Table(name = "forecast_snapshot")
public class ForecastSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ForecastRun run;

    @Column(nullable = false, length = 32)
    private String period;

    @Column
    private Integer year;

    @Column
    private Double measurementValue;

    @Column(length = 64)
    private String measurementLabel;

    @Column(length = 32)
    private String measurementUnit;

    @Column
    private Double predictedProduction;

    @Column
    private Double predictedYield;

    @Column
    private Double sownArea;

    @Column
    private Double averagePrice;

    @Column
    private Double estimatedRevenue;

    public ForecastRun getRun() {
        return run;
    }

    public void setRun(ForecastRun run) {
        this.run = run;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getMeasurementValue() {
        return measurementValue;
    }

    public void setMeasurementValue(Double measurementValue) {
        this.measurementValue = measurementValue;
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

    public Double getPredictedProduction() {
        return predictedProduction;
    }

    public void setPredictedProduction(Double predictedProduction) {
        this.predictedProduction = predictedProduction;
    }

    public Double getPredictedYield() {
        return predictedYield;
    }

    public void setPredictedYield(Double predictedYield) {
        this.predictedYield = predictedYield;
    }

    public Double getSownArea() {
        return sownArea;
    }

    public void setSownArea(Double sownArea) {
        this.sownArea = sownArea;
    }

    public Double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(Double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public Double getEstimatedRevenue() {
        return estimatedRevenue;
    }

    public void setEstimatedRevenue(Double estimatedRevenue) {
        this.estimatedRevenue = estimatedRevenue;
    }
}
