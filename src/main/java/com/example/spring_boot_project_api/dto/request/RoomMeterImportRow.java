package com.example.spring_boot_project_api.dto.request;

/**
 * One row of combined room + utility meter data imported through the rooms-with-meters
 * import flow.
 *
 * <p>Values are kept raw (strings) on purpose so that the same parsing + validation logic
 * used to build the preview is re-run server-side when the import is confirmed. No business
 * logic lives in this record.
 *
 * <p>The house, room type and meter install date are chosen once in the UI and travel at the
 * request level ({@link RoomMeterImportRequest}), not per row. Meter columns are optional:
 * a meter is created only when a non-blank meter number is provided; the meter install date
 * defaults to the request-level date (or the current date when blank) and the meter status is
 * handled by the existing meter creation rules when left blank.
 */
public record RoomMeterImportRow(
        String roomNumber,
        String name,
        String floorNumber,
        String monthlyRent,
        String securityDeposit,
        String status,
        String description,
        String electricityMeter,
        String electricityStatus,
        String waterMeter,
        String waterStatus
) {
    public static RoomMeterImportRow of(String roomNumber, String name, String floorNumber,
            String monthlyRent, String securityDeposit, String status, String description,
            String electricityMeter, String electricityStatus, String waterMeter,
            String waterStatus) {
        return new RoomMeterImportRow(
                trimToNull(roomNumber),
                trimToNull(name),
                trimToNull(floorNumber),
                trimToNull(monthlyRent),
                trimToNull(securityDeposit),
                trimToNull(status),
                trimToNull(description),
                trimToNull(electricityMeter),
                trimToNull(electricityStatus),
                trimToNull(waterMeter),
                trimToNull(waterStatus));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}