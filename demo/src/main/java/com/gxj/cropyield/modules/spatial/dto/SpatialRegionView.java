package com.gxj.cropyield.modules.spatial.dto;

import java.util.List;

/**
 * 区域指标视图。
 */
public class SpatialRegionView {

    private String name;

    private double production;

    private double forecastMin;

    private double forecastMax;

    private double risk;

    private List<Double> center;

    private String childKey;

    public SpatialRegionView(String name,
                              double production,
                              double forecastMin,
                              double forecastMax,
                              double risk,
                              List<Double> center,
                              String childKey) {
        this.name = name;
        this.production = production;
        this.forecastMin = forecastMin;
        this.forecastMax = forecastMax;
        this.risk = risk;
        this.center = center;
        this.childKey = childKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getProduction() {
        return production;
    }

    public void setProduction(double production) {
        this.production = production;
    }

    public double getForecastMin() {
        return forecastMin;
    }

    public void setForecastMin(double forecastMin) {
        this.forecastMin = forecastMin;
    }

    public double getForecastMax() {
        return forecastMax;
    }

    public void setForecastMax(double forecastMax) {
        this.forecastMax = forecastMax;
    }

    public double getRisk() {
        return risk;
    }

    public void setRisk(double risk) {
        this.risk = risk;
    }

    public List<Double> getCenter() {
        return center;
    }

    public void setCenter(List<Double> center) {
        this.center = center;
    }

    public String getChildKey() {
        return childKey;
    }

    public void setChildKey(String childKey) {
        this.childKey = childKey;
    }
}
