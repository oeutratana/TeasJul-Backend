package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record RoomImportPreviewResponse(
        String fileName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<RoomImportPreviewRowResponse> rows
) {
    public static RoomImportPreviewResponse of(String fileName, List<RoomImportPreviewRowResponse> rows) {
        int total = rows.size();
        int valid = (int) rows.stream().filter(RoomImportPreviewRowResponse::valid).count();
        return new RoomImportPreviewResponse(fileName, total, valid, total - valid, rows);
    }
}