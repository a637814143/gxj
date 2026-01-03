package com.gxj.cropyield.modules.spatial.dto;

import java.util.List;

/**
 * 返回空间分布地图层级结构的响应对象。
 */
public class SpatialMapResponse {

    private String rootKey;

    private List<SpatialMapView> maps;

    public SpatialMapResponse(String rootKey, List<SpatialMapView> maps) {
        this.rootKey = rootKey;
        this.maps = maps;
    }

    public String getRootKey() {
        return rootKey;
    }

    public void setRootKey(String rootKey) {
        this.rootKey = rootKey;
    }

    public List<SpatialMapView> getMaps() {
        return maps;
    }

    public void setMaps(List<SpatialMapView> maps) {
        this.maps = maps;
    }
}
