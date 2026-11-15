package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record RoomMeterImportPreviewResponse(
        String fileName,
        int totalRows,
        int validRows,
        int invalidRows,
        int roomsToCreate,
        int metersToCreate,
        List<RoomMeterImportPreviewRowResponse> rows
) {
    public static RoomMeterImportPreviewResponse of(String fileName,
            List<RoomMeterImportPreviewRowResponse> rows) {
        int total = rows.size();
        int valid = (int) rows.stream().filter(RoomMeterImportPreviewRowResponse::valid).count();
        int meters = rows.stream()
                .filter(RoomMeterImportPreviewRowResponse::valid)
                .mapToInt(RoomMeterImportPreviewRowResponse::metersToCreate)
                .sum();
        return new RoomMeterImportPreviewResponse(fileName, total, valid, total - valid, valid, meters, rows);
    }
}