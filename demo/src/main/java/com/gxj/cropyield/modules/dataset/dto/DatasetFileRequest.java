package com.gxj.cropyield.modules.dataset.dto;

import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DatasetFileRequest(
    @NotBlank(message = "数据集名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128位")
    String name,

    @NotNull(message = "数据集类型不能为空")
    DatasetType type,

    @NotBlank(message = "存储路径不能为空")
    @Size(max = 256, message = "存储路径长度不能超过256位")
    String storagePath,

    @Size(max = 256, message = "描述长度不能超过256位")
    String description
) {
}
