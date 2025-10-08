package com.dali.cropyield.modules.base.entity;

import com.dali.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ag_crop")
public class Crop extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(length = 64)
    private String unit;

    @Column(length = 256)
    private String description;
}
