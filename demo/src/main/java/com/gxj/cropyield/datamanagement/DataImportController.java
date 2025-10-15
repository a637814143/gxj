package com.gxj.cropyield.datamanagement;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/data-import")
public class DataImportController {

    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_DESCRIPTION_LENGTH = 256;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DataImportService dataImportService;
    private final DatasetFileService datasetFileService;
    private final Path storageRoot = Paths.get("uploads", "datasets");

    public DataImportController(DataImportService dataImportService, DatasetFileService datasetFileService) {
        this.dataImportService = dataImportService;
        this.datasetFileService = datasetFileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DataImportResponse> upload(@RequestPart("file") MultipartFile file,
                                                  @RequestParam(required = false) DatasetType type,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String description) {
        DatasetType datasetType = Optional.ofNullable(type).orElse(DatasetType.YIELD);
        String datasetName = determineDatasetName(name, file);

        Path savedPath = persistFile(datasetName, file);

        DataImportResponse response = dataImportService.importData(file);
        String finalDescription = resolveDescription(description, datasetName, response);

        datasetFileService.create(new DatasetFileRequest(datasetName, datasetType, savedPath.toString(), finalDescription));

        return ApiResponse.success(response);
    }

    private Path persistFile(String datasetName, MultipartFile file) {
        try {
            Files.createDirectories(storageRoot);
            String extension = resolveExtension(file.getOriginalFilename());
            String safeBase = datasetName.replaceAll("\\s+", "_");
            String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
            String filename = timestamp + "_" + safeBase + extension;
            Path target = storageRoot.resolve(filename);
            Files.write(target, file.getBytes());
            return target;
        } catch (IOException exception) {
            throw new IllegalArgumentException("保存导入文件失败", exception);
        }
    }

    private String resolveDescription(String description, String datasetName, DataImportResponse response) {
        String fallback = String.format(Locale.CHINA, "通过文件 %s 导入，新增 %d 条，更新 %d 条", datasetName,
                response.insertedRows(), response.updatedRows());
        String value = Optional.ofNullable(description)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(fallback);
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            return value.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        return value;
    }

    private String determineDatasetName(String providedName, MultipartFile file) {
        String candidate = Optional.ofNullable(providedName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> extractBaseName(file.getOriginalFilename()));
        if (candidate.length() > MAX_NAME_LENGTH) {
            return candidate.substring(0, MAX_NAME_LENGTH);
        }
        return candidate;
    }

    private String extractBaseName(String originalFilename) {
        String fallback = "数据集" + TIMESTAMP_FORMAT.format(LocalDateTime.now());
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        String trimmed = originalFilename.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        String base = dotIndex > 0 ? trimmed.substring(0, dotIndex) : trimmed;
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.isEmpty()) {
            return fallback;
        }
        return base;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".xlsx";
        }
        String lower = originalFilename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xls")) {
            return ".xls";
        }
        if (lower.endsWith(".xlsx")) {
            return ".xlsx";
        }
        return ".xlsx";
    }
}
