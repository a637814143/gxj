package com.gxj.cropyield.modules.consultation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 在线咨询模块的数据传输对象（记录类型），在创建咨询场景下承载参数。
 */
public record ConsultationCreateRequest(
    @NotBlank(message = "请选择作物类型") String cropType,
    @NotBlank(message = "请输入咨询主题") String subject,
    @NotBlank(message = "请填写问题详情") String description,
    @NotBlank(message = "请选择咨询部门") String departmentCode,
    String priority
) {
}
