package com.example.spring_boot_project_api.dto.response;

import java.util.List;

/**
 * Single parsed + validated room row returned by the preview endpoint. The raw fields are
 * returned as-is so the client can echo them back when confirming the import.
 */
public record RoomImportPreviewRowResponse(
        int rowNumber,
        String houseId,
        String roomTypeId,
        String roomNumber,
        String name,
        String floorNumber,
        String monthlyRent,
        String securityDeposit,
        String capacity,
        String status,
        String description,
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
    public static RoomImportPreviewRowResponse of(int rowNumber, String houseId, String roomTypeId,
            String roomNumber, String name, String floorNumber, String monthlyRent,
            String securityDeposit, String capacity, String status, String description,
            List<String> errors, List<String> warnings) {
        return new RoomImportPreviewRowResponse(rowNumber, houseId, roomTypeId, roomNumber, name,
                floorNumber, monthlyRent, securityDeposit, capacity, status, description,
                errors.isEmpty(), errors, warnings);
    }
}