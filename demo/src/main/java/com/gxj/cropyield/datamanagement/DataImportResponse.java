package com.gxj.cropyield.datamanagement;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordResponse;

import java.util.List;

public record DataImportResponse(
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        List<String> warnings,
        List<YieldRecordResponse> preview
) {
}
