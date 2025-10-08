package com.dali.cropyield.modules.dataset.entity;

import com.dali.cropyield.common.model.BaseEntity;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.entity.Region;
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
@Table(name = "ag_yield_record")
public class YieldRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Region region;

    @Column(nullable = false)
    private Integer year;

    @Column(precision = 16, scale = 4)
    private BigDecimal sownArea;

    @Column(precision = 16, scale = 4)
    private BigDecimal harvestedArea;

    @Column(precision = 16, scale = 4)
    private BigDecimal production;

    @Column(precision = 16, scale = 4)
    private BigDecimal yieldPerMu;

    @Column(length = 64)
    private String dataSource;

    @Column(length = 32)
    private String dataVersion;
}
