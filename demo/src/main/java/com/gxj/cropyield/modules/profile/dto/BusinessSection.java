package com.gxj.cropyield.modules.profile.dto;

import java.util.Map;
import java.util.List;

public record BusinessSection(
    Map<String, Object> form,
    List<CustomField> customFields
) {
}
