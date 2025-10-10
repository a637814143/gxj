package com.gxj.cropyield.modules.dataset.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.service.YieldRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/datasets/yield-records")
public class YieldRecordController {

    private final YieldRecordService yieldRecordService;

    public YieldRecordController(YieldRecordService yieldRecordService) {
        this.yieldRecordService = yieldRecordService;
    }

    @GetMapping
    public ApiResponse<List<YieldRecord>> listRecords() {
        return ApiResponse.success(yieldRecordService.listAll());
    }

    @PostMapping
    public ApiResponse<YieldRecord> createRecord(@Valid @RequestBody YieldRecordRequest request) {
        return ApiResponse.success(yieldRecordService.create(request));
    }
}
