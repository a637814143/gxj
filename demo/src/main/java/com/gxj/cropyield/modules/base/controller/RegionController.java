package com.gxj.cropyield.modules.base.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.service.RegionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
