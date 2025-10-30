package com.gxj.cropyield.modules.base.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.enums.HarvestSeason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
/**
 * 基础数据模块的实体类，映射基础数据领域对应的数据表结构。
 */

@Entity
@Table(name = "base_crop")
public class Crop extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(length = 256)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "harvest_season", nullable = false, length = 32)
    private HarvestSeason harvestSeason = HarvestSeason.ANNUAL;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public HarvestSeason getHarvestSeason() {
        return harvestSeason;
    }

    public void setHarvestSeason(HarvestSeason harvestSeason) {
        this.harvestSeason = harvestSeason != null ? harvestSeason : HarvestSeason.ANNUAL;
    }
}
