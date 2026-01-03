package com.gxj.cropyield.modules.forecast.entity;

import com.gxj.cropyield.common.converter.JsonMapConverter;
import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;
/**
 * 预测管理模块的实体类，映射预测管理领域对应的数据表结构。
 */

@Entity
@Table(name = "forecast_model")
public class ForecastModel extends BaseEntity {

    public enum ModelType {
        LSTM,
        WEATHER_REGRESSION,
        ARIMA,
        PROPHET
    }

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModelType type;

    @Column(length = 512)
    private String description;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "parameters", columnDefinition = "TEXT")
    private Map<String, Object> parameters = new HashMap<>();

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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
    }
}
