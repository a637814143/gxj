package com.gxj.cropyield.modules.base.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 基础数据模块的区域可见性调整请求。
 */
public record RegionVisibilityRequest(
    @NotNull(message = "隐藏状态不能为空")
    Boolean hidden
) {
}
