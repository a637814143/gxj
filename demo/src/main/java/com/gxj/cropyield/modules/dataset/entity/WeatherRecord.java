package com.gxj.cropyield.modules.dataset.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.base.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 气象数据记录实体，对应 dataset_weather_record 表。
 */
@Entity
@Table(name = "dataset_weather_record")
public class WeatherRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_file_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private DatasetFile datasetFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "record_date", nullable = false)
    private java.time.LocalDate recordDate;

    @Column(name = "max_temperature")
    private Double maxTemperature;

    @Column(name = "min_temperature")
    private Double minTemperature;

    @Column(name = "weather_text", length = 128)
    private String weatherText;

    @Column(length = 128)
    private String wind;

    @Column(name = "sunshine_hours")
    private Double sunshineHours;

    @Column(name = "data_source", length = 256)
    private String dataSource;

    public DatasetFile getDatasetFile() {
        return datasetFile;
    }

    public void setDatasetFile(DatasetFile datasetFile) {
        this.datasetFile = datasetFile;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public java.time.LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(java.time.LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public Double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public Double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public String getWeatherText() {
        return weatherText;
    }

    public void setWeatherText(String weatherText) {
        this.weatherText = weatherText;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public Double getSunshineHours() {
        return sunshineHours;
    }

    public void setSunshineHours(Double sunshineHours) {
        this.sunshineHours = sunshineHours;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
