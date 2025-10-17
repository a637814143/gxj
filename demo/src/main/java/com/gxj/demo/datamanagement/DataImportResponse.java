package com.gxj.demo.datamanagement;

import com.gxj.demo.yielddata.YieldRecordResponse;

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
