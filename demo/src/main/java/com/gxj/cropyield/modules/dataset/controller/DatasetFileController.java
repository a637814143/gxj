package com.gxj.cropyield.modules.dataset.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 数据集管理模块的控制器，用于暴露数据集管理相关的 REST 接口。
 * <p>核心方法：listFiles、createFile、deleteFiles。</p>
 */

@RestController
@RequestMapping("/api/datasets/files")
public class DatasetFileController {

    private final DatasetFileService datasetFileService;

    public DatasetFileController(DatasetFileService datasetFileService) {
        this.datasetFileService = datasetFileService;
    }

    @GetMapping
    public ApiResponse<List<DatasetFile>> listFiles() {
        return ApiResponse.success(datasetFileService.listAll());
    }

    @PostMapping
    public ApiResponse<DatasetFile> createFile(@Valid @RequestBody DatasetFileRequest request) {
        return ApiResponse.success(datasetFileService.create(request));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteFiles(@RequestBody List<Long> ids) {
        datasetFileService.deleteByIds(ids);
        return ApiResponse.success();
    }
}
