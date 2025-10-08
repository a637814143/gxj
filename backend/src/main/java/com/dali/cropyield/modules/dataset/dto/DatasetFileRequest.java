package com.dali.cropyield.modules.dataset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DatasetFileRequest(
        @NotBlank @Size(max = 128) String fileName,
        @NotBlank @Size(max = 64) String fileType,
        @Size(max = 256) String storagePath,
        @Size(max = 256) String description,
        LocalDate importedAt) {
}
