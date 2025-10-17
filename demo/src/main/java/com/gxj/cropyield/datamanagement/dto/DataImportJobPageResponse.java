package com.gxj.cropyield.datamanagement.dto;

import java.util.List;

public record DataImportJobPageResponse(
        List<DataImportJobView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
