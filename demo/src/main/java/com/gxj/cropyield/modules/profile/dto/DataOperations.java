package com.gxj.cropyield.modules.profile.dto;

import java.util.List;

public record DataOperations(
    List<String> lastSelection,
    List<DataCleanupRecord> history,
    String lastExportTime,
    ExportForm exportForm
) {
}
