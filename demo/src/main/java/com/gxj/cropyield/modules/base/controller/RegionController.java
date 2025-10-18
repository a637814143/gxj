package com.gxj.cropyield.modules.base.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.service.RegionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public ApiResponse<List<Region>> listRegions() {
        return ApiResponse.success(regionService.listAll());
    }

    @PostMapping
    public ApiResponse<Region> createRegion(@Valid @RequestBody RegionRequest request) {
        return ApiResponse.success(regionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Region> updateRegion(@PathVariable Long id, @Valid @RequestBody RegionRequest request) {
        return ApiResponse.success(regionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRegion(@PathVariable Long id) {
        regionService.delete(id);
        return ApiResponse.success();
    }
}
