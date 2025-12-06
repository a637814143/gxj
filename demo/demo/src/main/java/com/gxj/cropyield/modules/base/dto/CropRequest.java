package com.gxj.cropyield.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * 基础数据模块的数据传输对象（记录类型），在基础数据场景下承载参数与返回值。
 */

public record CropRequest(
    @NotBlank(message = "作物编码不能为空")
    @Size(max = 64, message = "编码长度不能超过64位")
    String code,

    @NotBlank(message = "作物名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128位")
    String name,

    @Size(max = 64, message = "分类长度不能超过64位")
    String category,

    @Size(max = 256, message = "描述长度不能超过256位")
    String description
) {
}
