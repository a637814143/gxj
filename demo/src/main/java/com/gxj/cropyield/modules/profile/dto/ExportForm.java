package com.gxj.cropyield.modules.profile.dto;

import java.util.List;

public record ExportForm(
    String scope,
    String format,
    List<String> range,
    Boolean includeAudit
) {
}
