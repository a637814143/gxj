package com.gxj.cropyield.modules.spatial;

import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.spatial.dto.SpatialHeatmapPoint;
import com.gxj.cropyield.modules.spatial.dto.SpatialMapResponse;
import com.gxj.cropyield.modules.spatial.dto.SpatialMapView;
import com.gxj.cropyield.modules.spatial.dto.SpatialRegionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 空间分布数据服务：从基础区域和历史产量数据生成地图层级、区域指标与热力点。
 */
@Service
public class SpatialMapService {

    private static final Logger log = LoggerFactory.getLogger(SpatialMapService.class);

    private final RegionRepository regionRepository;
    private final YieldRecordRepository yieldRecordRepository;

    public SpatialMapService(RegionRepository regionRepository, YieldRecordRepository yieldRecordRepository) {
        this.regionRepository = regionRepository;
        this.yieldRecordRepository = yieldRecordRepository;
    }

    public SpatialMapResponse getSpatialMaps() {
        List<Region> regions = regionRepository.findAll();
        List<YieldRecord> yieldRecords = yieldRecordRepository.findAll();

        if (regions.isEmpty()) {
            log.warn("未找到区域基础数据，返回空的空间分布结果");
            return new SpatialMapResponse(null, List.of());
        }

        Map<Long, DoubleSummaryStatistics> yieldByRegion = yieldRecords.stream()
                .collect(Collectors.groupingBy(record -> record.getRegion().getId(),
                        Collectors.summarizingDouble(record -> Optional.ofNullable(record.getYieldPerHectare())
                                .orElseGet(() -> Optional.ofNullable(record.getProduction()).orElse(0.0)))));

        double maxYield = yieldByRegion.values().stream()
                .mapToDouble(DoubleSummaryStatistics::getAverage)
                .max()
                .orElse(0.0);

        Map<String, List<Region>> childrenByParent = regions.stream()
                .collect(Collectors.groupingBy(region -> Optional.ofNullable(region.getParentCode()).orElse("ROOT")));

        String rootKey = "root-regions";
        List<SpatialMapView> maps = new ArrayList<>();

        // 根级地图
        List<Region> rootRegions = childrenByParent.getOrDefault("ROOT", List.of());
        maps.add(buildMapView(rootKey, null, "行政区划概览", "基于基础区域与历史产量生成的空间分布", rootRegions, yieldByRegion, maxYield, childrenByParent));

        // 下钻地图
        for (Region region : regions) {
            List<Region> children = childrenByParent.get(region.getCode());
            if (children != null && !children.isEmpty()) {
                maps.add(buildMapView(region.getCode(), rootKey, region.getName(), safeDescription(region), children, yieldByRegion, maxYield, childrenByParent));
            }
        }

        return new SpatialMapResponse(rootKey, maps);
    }

    private SpatialMapView buildMapView(String key,
                                         String parentKey,
                                         String label,
                                         String description,
                                         List<Region> regions,
                                         Map<Long, DoubleSummaryStatistics> yieldByRegion,
                                         double maxYield,
                                         Map<String, List<Region>> childrenByParent) {
        Map<String, Object> geoJson = buildGeoJson(key, regions);
        List<SpatialRegionView> regionViews = new ArrayList<>();
        List<SpatialHeatmapPoint> heatmapPoints = new ArrayList<>();

        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            List<Double> center = buildCenter(i, key.hashCode());
            DoubleSummaryStatistics stats = yieldByRegion.get(region.getId());
            double production = stats != null ? stats.getAverage() : 0.0;
            double forecastMin = production * 0.95;
            double forecastMax = production * 1.05;
            double risk = maxYield > 0 ? 1 - Math.min(1, production / maxYield) : 0.4;
            String childKey = childrenByParent.containsKey(region.getCode()) ? region.getCode() : null;

            regionViews.add(new SpatialRegionView(
                    region.getName(),
                    round(production),
                    round(forecastMin),
                    round(forecastMax),
                    clamp(risk),
                    center,
                    childKey
            ));

            heatmapPoints.add(new SpatialHeatmapPoint(region.getName(), List.of(center.get(0), center.get(1), clamp(risk))));
        }

        return new SpatialMapView(
                key,
                parentKey,
                "map-" + key,
                label,
                description,
                geoJson,
                regionViews,
                heatmapPoints
        );
    }

    private Map<String, Object> buildGeoJson(String key, List<Region> regions) {
        Map<String, Object> geoJson = new LinkedHashMap<>();
        geoJson.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            List<Double> center = buildCenter(i, key.hashCode());
            double lng = center.get(0);
            double lat = center.get(1);
            double width = 0.8;
            double height = 0.6;

            List<List<List<Double>>> polygon = List.of(List.of(
                    List.of(lng - width, lat - height),
                    List.of(lng + width, lat - height),
                    List.of(lng + width, lat + height),
                    List.of(lng - width, lat + height),
                    List.of(lng - width, lat - height)
            ));

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", region.getName());
            properties.put("code", region.getCode());
            properties.put("level", region.getLevel());
            feature.put("properties", properties);

            Map<String, Object> geometry = new LinkedHashMap<>();
            geometry.put("type", "Polygon");
            geometry.put("coordinates", List.of(polygon));
            feature.put("geometry", geometry);

            features.add(feature);
        }

        geoJson.put("features", features);
        return geoJson;
    }

    private List<Double> buildCenter(int index, int seed) {
        double baseLng = 100 + (index % 6) * 3 + (seed % 3) * 0.2;
        double baseLat = 25 + (index / 6) * 2.5 + (Math.abs(seed) % 5) * 0.1;
        return List.of(round(baseLng), round(baseLat));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double clamp(double value) {
        return Math.min(1.0, Math.max(0.0, round(value)));
    }

    private String safeDescription(Region region) {
        String level = Optional.ofNullable(region.getLevel()).orElse("").toLowerCase(Locale.ROOT);
        return switch (level) {
            case "province", "city" -> region.getName() + " 区域下钻";
            default -> "下钻至 " + region.getName();
        };
    }
}
