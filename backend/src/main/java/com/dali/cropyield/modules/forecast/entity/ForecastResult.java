package com.dali.cropyield.modules.forecast.entity;

import com.dali.cropyield.common.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fc_result")
public class ForecastResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private ForecastTask task;

    @Column(nullable = false)
    private Integer targetYear;

    @Column(precision = 16, scale = 4)
    private BigDecimal predictedProduction;

    @Column(precision = 16, scale = 4)
    private BigDecimal predictedYieldPerMu;

    @Column(precision = 16, scale = 4)
    private BigDecimal predictedRevenue;

    @Column(precision = 16, scale = 4)
    private BigDecimal confidenceLower;

    @Column(precision = 16, scale = 4)
    private BigDecimal confidenceUpper;
}
