package com.example.spring_boot_project_api.dto.response;

import java.util.List;

/**
 * Single parsed + validated combined room + meters row returned by the preview endpoint.
 * The raw fields are returned as-is so the client can echo them back when confirming.
 */
public record RoomMeterImportPreviewRowResponse(
        int rowNumber,
        String house,
        String roomType,
        String roomNumber,
        String name,
        String floorNumber,
        String monthlyRent,
        String securityDeposit,
        String status,
        String description,
        String electricityMeter,
        String waterMeter,
        int metersToCreate,
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
    public static RoomMeterImportPreviewRowResponse of(int rowNumber, String house, String roomType,
            String roomNumber, String name, String floorNumber, String monthlyRent,
            String securityDeposit, String status, String description,
                        String electricityMeter, String waterMeter,
            int metersToCreate, List<String> errors, List<String> warnings) {
        return new RoomMeterImportPreviewRowResponse(rowNumber, house, roomType, roomNumber, name,
                floorNumber, monthlyRent, securityDeposit, status, description,
                                electricityMeter, waterMeter, metersToCreate,
                errors.isEmpty(), errors, warnings);
    }
}