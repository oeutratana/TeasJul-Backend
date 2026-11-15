package com.example.spring_boot_project_api.dto.response;

import java.util.List;

/**
 * Single parsed + validated utility meter row returned by the preview endpoint. The raw fields are
 * returned as-is so the client can echo them back when confirming the import.
 */
public record MeterImportPreviewRowResponse(
        int rowNumber,
        String houseId,
        String roomId,
        String utilityTypeId,
        String meterNumber,
        String installedAt,
        String status,
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
    public static MeterImportPreviewRowResponse of(int rowNumber, String houseId, String roomId,
            String utilityTypeId, String meterNumber, String installedAt, String status,
            List<String> errors, List<String> warnings) {
        return new MeterImportPreviewRowResponse(rowNumber, houseId, roomId, utilityTypeId,
                meterNumber, installedAt, status, errors.isEmpty(), errors, warnings);
    }
}