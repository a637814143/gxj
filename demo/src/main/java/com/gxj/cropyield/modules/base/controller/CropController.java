package com.gxj.cropyield.modules.base.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.base.dto.CropRequest;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.service.CropService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 基础数据模块的控制器，用于暴露基础数据相关的 REST 接口。
 * <p>核心方法：listCrops、createCrop。</p>
 */

/**
 * 作物管理控制器 - 作物基础数据管理
 */
@RestController
@RequestMapping("/api/base/crops")
public class CropController {

    private final CropService cropService;

    public CropController(CropService cropService) {
        this.cropService = cropService;
    }

    @GetMapping
    public ApiResponse<List<Crop>> listCrops() {
        return ApiResponse.success(cropService.listAll());
    }

    @PostMapping
    public ApiResponse<Crop> createCrop(@Valid @RequestBody CropRequest request) {
        return ApiResponse.success(cropService.create(request));
    }
}
