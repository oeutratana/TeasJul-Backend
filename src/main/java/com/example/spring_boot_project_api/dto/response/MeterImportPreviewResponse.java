package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record MeterImportPreviewResponse(
        String fileName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<MeterImportPreviewRowResponse> rows
) {
    public static MeterImportPreviewResponse of(String fileName, List<MeterImportPreviewRowResponse> rows) {
        int total = rows.size();
        int valid = (int) rows.stream().filter(MeterImportPreviewRowResponse::valid).count();
        return new MeterImportPreviewResponse(fileName, total, valid, total - valid, rows);
    }
}