package com.gxj.cropyield.modules.dataset.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.PriceRecord;
import com.gxj.cropyield.modules.dataset.service.PriceRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 数据集管理模块的控制器，用于暴露数据集管理相关的 REST 接口。
 * <p>核心方法：listRecords、createRecord。</p>
 */

@RestController
@RequestMapping("/api/datasets/price-records")
public class PriceRecordController {

    private final PriceRecordService priceRecordService;

    public PriceRecordController(PriceRecordService priceRecordService) {
        this.priceRecordService = priceRecordService;
    }

    @GetMapping
    public ApiResponse<List<PriceRecord>> listRecords() {
        return ApiResponse.success(priceRecordService.listAll());
    }

    @PostMapping
    public ApiResponse<PriceRecord> createRecord(@Valid @RequestBody PriceRecordRequest request) {
        return ApiResponse.success(priceRecordService.create(request));
    }
}
