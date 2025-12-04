package com.gxj.cropyield.modules.dataset.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
/**
 * 数据集管理模块的实体类，映射数据集管理领域对应的数据表结构。
 */

@Entity
@Table(name = "dataset_file")
public class DatasetFile extends BaseEntity {

    public enum DatasetType {
        YIELD,
        PRICE,
        WEATHER,
        SOIL
    }

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DatasetType type;

    @Column(nullable = false, length = 256)
    private String storagePath;

    @Column(length = 256)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatasetType getType() {
        return type;
    }

    public void setType(DatasetType type) {
        this.type = type;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
