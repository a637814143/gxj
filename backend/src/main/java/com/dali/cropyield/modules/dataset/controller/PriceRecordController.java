package com.dali.cropyield.modules.dataset.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.dali.cropyield.modules.dataset.entity.PriceRecord;
import com.dali.cropyield.modules.dataset.service.PriceRecordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datasets/prices")
@RequiredArgsConstructor
public class PriceRecordController {

    private final PriceRecordService priceRecordService;

    @GetMapping
    public ApiResponse<List<PriceRecord>> list() {
        return ApiResponse.success(priceRecordService.findAll());
    }

    @PostMapping
    public ApiResponse<PriceRecord> create(@Valid @RequestBody PriceRecordRequest request) {
        return ApiResponse.success(priceRecordService.create(request));
    }
}
