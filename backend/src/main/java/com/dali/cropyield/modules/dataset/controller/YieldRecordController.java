package com.dali.cropyield.modules.dataset.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.dali.cropyield.modules.dataset.entity.YieldRecord;
import com.dali.cropyield.modules.dataset.service.YieldRecordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datasets/yields")
@RequiredArgsConstructor
public class YieldRecordController {

    private final YieldRecordService yieldRecordService;

    @GetMapping
    public ApiResponse<List<YieldRecord>> list() {
        return ApiResponse.success(yieldRecordService.findAll());
    }

    @PostMapping
    public ApiResponse<YieldRecord> create(@Valid @RequestBody YieldRecordRequest request) {
        return ApiResponse.success(yieldRecordService.create(request));
    }
}
