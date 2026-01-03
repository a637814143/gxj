package com.gxj.cropyield.modules.spatial.dto;

import java.util.List;

/**
 * 热力点定义。
 */
public class SpatialHeatmapPoint {

    private String name;

    private List<Double> value;

    public SpatialHeatmapPoint(String name, List<Double> value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Double> getValue() {
        return value;
    }

    public void setValue(List<Double> value) {
        this.value = value;
    }
}
