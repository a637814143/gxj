package com.dali.cropyield.modules.dataset.entity;

import com.dali.cropyield.common.model.BaseEntity;
import com.dali.cropyield.modules.base.entity.Crop;
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
@Table(name = "ag_price_record")
public class PriceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Crop crop;

    @Column(nullable = false)
    private Integer year;

    @Column(precision = 16, scale = 4)
    private BigDecimal averagePrice;

    @Column(length = 32)
    private String unit;

    @Column(length = 64)
    private String dataSource;
}
