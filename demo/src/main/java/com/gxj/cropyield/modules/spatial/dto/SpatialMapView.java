package com.gxj.cropyield.modules.spatial.dto;

import java.util.List;
import java.util.Map;

/**
 * 地图层级定义，包括 GeoJSON、区域指标与热力点。
 */
public class SpatialMapView {

    private String key;

    private String parentKey;

    private String mapKey;

    private String label;

    private String description;

    private Map<String, Object> geoJson;

    private List<SpatialRegionView> regions;

    private List<SpatialHeatmapPoint> heatmapPoints;

    public SpatialMapView(String key,
                          String parentKey,
                          String mapKey,
                          String label,
                          String description,
                          Map<String, Object> geoJson,
                          List<SpatialRegionView> regions,
                          List<SpatialHeatmapPoint> heatmapPoints) {
        this.key = key;
        this.parentKey = parentKey;
        this.mapKey = mapKey;
        this.label = label;
        this.description = description;
        this.geoJson = geoJson;
        this.regions = regions;
        this.heatmapPoints = heatmapPoints;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getGeoJson() {
        return geoJson;
    }

    public void setGeoJson(Map<String, Object> geoJson) {
        this.geoJson = geoJson;
    }

    public List<SpatialRegionView> getRegions() {
        return regions;
    }

    public void setRegions(List<SpatialRegionView> regions) {
        this.regions = regions;
    }

    public List<SpatialHeatmapPoint> getHeatmapPoints() {
        return heatmapPoints;
    }

    public void setHeatmapPoints(List<SpatialHeatmapPoint> heatmapPoints) {
        this.heatmapPoints = heatmapPoints;
    }
}
