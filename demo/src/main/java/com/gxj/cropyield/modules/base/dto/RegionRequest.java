package com.gxj.cropyield.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionRequest(
    @NotBlank(message = "区域编码不能为空")
    @Size(max = 64, message = "编码长度不能超过64位")
    String code,

    @NotBlank(message = "区域名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128位")
    String name,

    @Size(max = 256, message = "描述长度不能超过256位")
    String description
) {
}
