package com.gxj.demo.yielddata;

import com.gxj.demo.crop.Crop;
import com.gxj.demo.region.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity(name = "LegacyYieldRecord")
@Table(name = "yield_records")
public class YieldRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "harvest_year", nullable = false)
    private Integer year;

    @Column(name = "sown_area")
    private Double sownArea;

    @Column(nullable = false)
    private Double production;

    @Column(name = "yield_per_hectare")
    private Double yieldPerHectare;

    @Column(name = "average_price")
    private Double averagePrice;

    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "collected_at")
    private LocalDate collectedAt;

    protected YieldRecord() {
    }

    public YieldRecord(
            Crop crop,
            Region region,
            Integer year,
            Double sownArea,
            Double production,
            Double yieldPerHectare,
            Double averagePrice,
            String dataSource,
            LocalDate collectedAt
    ) {
        this.crop = crop;
        this.region = region;
        this.year = year;
        this.sownArea = sownArea;
        this.production = production;
        this.yieldPerHectare = yieldPerHectare;
        this.averagePrice = averagePrice;
        this.dataSource = dataSource;
        this.collectedAt = collectedAt;
    }

    public Long getId() {
        return id;
    }

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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public Double getYieldPerHectare() {
        return yieldPerHectare;
    }

    public void setYieldPerHectare(Double yieldPerHectare) {
        this.yieldPerHectare = yieldPerHectare;
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

    public LocalDate getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDate collectedAt) {
        this.collectedAt = collectedAt;
    }
}
