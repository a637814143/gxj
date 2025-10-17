package com.gxj.cropyield.datamanagement.dto;

public record DataImportErrorView(
        int rowNumber,
        String errorCode,
        String message,
        String rawValue
) {
}
