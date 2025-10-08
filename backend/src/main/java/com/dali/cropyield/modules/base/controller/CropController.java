package com.dali.cropyield.modules.base.controller;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.base.dto.CropRequest;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.service.CropService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @GetMapping
    public ApiResponse<List<Crop>> list() {
        return ApiResponse.success(cropService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Crop> detail(@PathVariable Long id) {
        Crop crop = cropService.findById(id)
                .orElseThrow(() -> BusinessException.notFound("作物不存在"));
        return ApiResponse.success(crop);
    }

    @PostMapping
    public ApiResponse<Crop> create(@Valid @RequestBody CropRequest request) {
        return ApiResponse.success(cropService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Crop> update(@PathVariable Long id, @Valid @RequestBody CropRequest request) {
        return ApiResponse.success(cropService.update(id, request));
    }
}
