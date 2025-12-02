package com.gxj.cropyield.modules.consultation.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 咨询部门定义枚举。
 */
public enum ConsultationDepartment {
    PLANT_PROTECTION("PLANT_PROTECTION", "植保与防灾中心", "提供病虫害诊断、防控策略与农技指导"),
    SOIL_SERVICE("SOIL_SERVICE", "土壤肥料服务站", "为土壤改良、施肥配方和测土配肥提供建议"),
    MARKET_SERVICE("MARKET_SERVICE", "农产品市场科", "关注销售渠道与产销对接"),
    SMART_AGRI("SMART_AGRI", "智慧农机中心", "提供农机设备维护、智能农机应用支持");

    private final String code;
    private final String name;
    private final String description;

    ConsultationDepartment(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<ConsultationDepartment> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase();
        return Arrays.stream(values())
            .filter(item -> item.code.equalsIgnoreCase(normalized))
            .findFirst();
    }

    public static List<DepartmentOption> toOptions() {
        return Arrays.stream(values())
            .map(item -> new DepartmentOption(item.code, item.name, item.description))
            .collect(Collectors.toList());
    }

    /**
     * 部门选项展示模型。
     */
    public record DepartmentOption(String code, String name, String description) {
    }
}
