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
@Table(name = "forecast_run_series")
public class ForecastRunSeries extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ForecastRun run;

    @Column(nullable = false, length = 32)
    private String period;

    @Column(nullable = false)
    private Double value;

    @Column
    private Double lowerBound;

    @Column
    private Double upperBound;

    @Column(nullable = false)
    private Boolean historical;

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

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double upperBound) {
        this.upperBound = upperBound;
    }

    public Boolean getHistorical() {
        return historical;
    }

    public void setHistorical(Boolean historical) {
        this.historical = historical;
    }
}
