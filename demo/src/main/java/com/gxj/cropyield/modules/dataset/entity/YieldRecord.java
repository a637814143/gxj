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

@Entity
@Table(name = "dataset_yield_record")
public class YieldRecord extends BaseEntity {

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
    private Integer year;

    @Column(name = "sown_area")
    private Double sownArea;

    @Column
    private Double production;

    @Column(name = "yield_per_hectare")
    private Double yieldPerHectare;

    @Column(name = "average_price")
    private Double averagePrice;

    @Column(name = "data_source", length = 256)
    private String dataSource;

    @Column(name = "collected_at")
    private java.time.LocalDate collectedAt;

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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getYieldPerHectare() {
        return yieldPerHectare;
    }

    public void setYieldPerHectare(Double yieldPerHectare) {
        this.yieldPerHectare = yieldPerHectare;
    }

    public Double getSownArea() {
        return sownArea;
    }

    public void setSownArea(Double sownArea) {
        this.sownArea = sownArea;
    }

    public Double getProduction() {
        return production;
    }

    public void setProduction(Double production) {
        this.production = production;
    }

    public Double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(Double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public java.time.LocalDate getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(java.time.LocalDate collectedAt) {
        this.collectedAt = collectedAt;
    }
}
