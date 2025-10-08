package com.gxj.cropyield.modules.dataset.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
