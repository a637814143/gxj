package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "forecast_result")
public class ForecastResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ForecastTask task;

    @Column(nullable = false)
    private Integer targetYear;

    @Column
    private Double predictedYield;

    @Column
    private Double measurementValue;

    @Column(length = 64)
    private String measurementLabel;

    @Column(length = 32)
    private String measurementUnit;

    @Column
    private Double predictedProduction;

    @Column(length = 1024)
    private String evaluation;

    public ForecastTask getTask() {
        return task;
    }

    public void setTask(ForecastTask task) {
        this.task = task;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }

    public Double getPredictedYield() {
        return predictedYield;
    }

    public void setPredictedYield(Double predictedYield) {
        this.predictedYield = predictedYield;
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

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}
