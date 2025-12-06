package com.gxj.cropyield.modules.consultation.dto;

/**
 * 前端展示与业务逻辑使用的咨询部门选项。
 */
public record ConsultationDepartmentOption(
    String code,
    String name,
    String description,
    Long contactUserId,
    String contactUsername,
    String contactName
) {
}
