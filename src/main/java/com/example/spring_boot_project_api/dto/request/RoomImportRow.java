package com.example.spring_boot_project_api.dto.request;

/**
 * One row of room data imported from a CSV/Excel file through the room import flow.
 *
 * <p>Values are kept raw (strings) on purpose so that the same parsing + validation logic
 * used to build the preview is re-run server-side when the import is confirmed. No business
 * logic lives in this record.
 */
public record RoomImportRow(
        String houseId,
        String roomTypeId,
        String roomNumber,
        String name,
        String floorNumber,
        String monthlyRent,
        String securityDeposit,
        String capacity,
        String status,
        String description
) {
    public static RoomImportRow of(String houseId, String roomTypeId, String roomNumber, String name,
            String floorNumber, String monthlyRent, String securityDeposit, String capacity,
            String status, String description) {
        return new RoomImportRow(
                trimToNull(houseId),
                trimToNull(roomTypeId),
                trimToNull(roomNumber),
                trimToNull(name),
                trimToNull(floorNumber),
                trimToNull(monthlyRent),
                trimToNull(securityDeposit),
                trimToNull(capacity),
                trimToNull(status),
                trimToNull(description));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}