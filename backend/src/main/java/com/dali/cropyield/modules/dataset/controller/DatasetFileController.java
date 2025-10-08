package com.dali.cropyield.modules.dataset.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.dali.cropyield.modules.dataset.entity.DatasetFile;
import com.dali.cropyield.modules.dataset.service.DatasetFileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datasets/files")
@RequiredArgsConstructor
public class DatasetFileController {

    private final DatasetFileService datasetFileService;

    @GetMapping
    public ApiResponse<List<DatasetFile>> list() {
        return ApiResponse.success(datasetFileService.findAll());
    }

    @PostMapping
    public ApiResponse<DatasetFile> create(@Valid @RequestBody DatasetFileRequest request) {
        DatasetFile file = new DatasetFile();
        file.setFileName(request.fileName());
        file.setFileType(request.fileType());
        file.setStoragePath(request.storagePath());
        file.setDescription(request.description());
        file.setImportedAt(request.importedAt());
        return ApiResponse.success(datasetFileService.save(file));
    }
}
