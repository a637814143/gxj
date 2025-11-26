package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
/**
 * 预测管理模块的实体类，映射预测管理领域对应的数据表结构。
 */

@Entity
@Table(name = "forecast_model")
public class ForecastModel extends BaseEntity {

    public enum ModelType {
        TIME_SERIES,
        MACHINE_LEARNING,
        LSTM,
        WEATHER_REGRESSION
    }

    public enum Granularity {
        YEARLY,
        QUARTERLY,
        MONTHLY
    }

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModelType type;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "granularity", nullable = false, length = 32)
    private Granularity granularity = Granularity.YEARLY;

    @Column(name = "history_window", nullable = false)
    private Integer historyWindow = 5;

    @Column(name = "forecast_horizon", nullable = false)
    private Integer forecastHorizon = 1;

    @Column(name = "crop_scope", nullable = false, length = 128)
    private String cropScope = "全部作物";

    @Column(name = "region_scope", nullable = false, length = 128)
    private String regionScope = "全部区域";

    @Column(name = "hyper_parameters", length = 512)
    private String hyperParameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }

    public Integer getHistoryWindow() {
        return historyWindow;
    }

    public void setHistoryWindow(Integer historyWindow) {
        this.historyWindow = historyWindow;
    }

    public Integer getForecastHorizon() {
        return forecastHorizon;
    }

    public void setForecastHorizon(Integer forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }

    public String getCropScope() {
        return cropScope;
    }

    public void setCropScope(String cropScope) {
        this.cropScope = cropScope;
    }

    public String getRegionScope() {
        return regionScope;
    }

    public void setRegionScope(String regionScope) {
        this.regionScope = regionScope;
    }

    public String getHyperParameters() {
        return hyperParameters;
    }

    public void setHyperParameters(String hyperParameters) {
        this.hyperParameters = hyperParameters;
    }
}
