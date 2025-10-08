package com.dali.cropyield.modules.base.controller;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.base.dto.RegionRequest;
import com.dali.cropyield.modules.base.entity.Region;
import com.dali.cropyield.modules.base.service.RegionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public ApiResponse<List<Region>> list(@RequestParam(required = false) String level) {
        List<Region> regions = level == null ? regionService.findAll() : regionService.findByLevel(level);
        return ApiResponse.success(regions);
    }

    @GetMapping("/{id}")
    public ApiResponse<Region> detail(@PathVariable Long id) {
        Region region = regionService.findById(id)
                .orElseThrow(() -> BusinessException.notFound("地区不存在"));
        return ApiResponse.success(region);
    }

    @PostMapping
    public ApiResponse<Region> create(@Valid @RequestBody RegionRequest request) {
        return ApiResponse.success(regionService.create(request));
    }
}
