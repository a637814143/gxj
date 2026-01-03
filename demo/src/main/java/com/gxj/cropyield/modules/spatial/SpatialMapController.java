package com.gxj.cropyield.modules.spatial;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.spatial.dto.SpatialMapResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外暴露空间分布地图数据的接口。
 */
@RestController
@RequestMapping({"/api/spatial", "/spatial"})
public class SpatialMapController {

    private final SpatialMapService spatialMapService;

    public SpatialMapController(SpatialMapService spatialMapService) {
        this.spatialMapService = spatialMapService;
    }

    @GetMapping("/maps")
    public ApiResponse<SpatialMapResponse> getMaps() {
        return ApiResponse.success(spatialMapService.getSpatialMaps());
    }
}
