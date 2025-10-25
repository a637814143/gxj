package com.gxj.cropyield.modules.dataset.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
/**
 * 数据集管理模块的实体类，映射数据集管理领域对应的数据表结构。
 */

@Entity
@Table(name = "dataset_price_record")
public class PriceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_file_id")
    private DatasetFile datasetFile;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private Double price;

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public DatasetFile getDatasetFile() {
        return datasetFile;
    }

    public void setDatasetFile(DatasetFile datasetFile) {
        this.datasetFile = datasetFile;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
