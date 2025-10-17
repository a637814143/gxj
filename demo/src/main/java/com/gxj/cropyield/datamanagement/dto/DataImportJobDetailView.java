package com.gxj.cropyield.datamanagement.dto;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordResponse;

import java.util.List;

public record DataImportJobDetailView(
        DataImportJobView summary,
        List<String> warnings,
        List<DataImportErrorView> errors,
        List<YieldRecordResponse> preview
) {
}
