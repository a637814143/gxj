package com.dali.cropyield.modules.forecast.entity;

import com.dali.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fc_model")
public class ForecastModel extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false, length = 64)
    private String algorithm;

    @Column(length = 256)
    private String description;

    @Lob
    private String hyperParameters;
}
