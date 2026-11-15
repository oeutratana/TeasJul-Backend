package com.example.spring_boot_project_api.dto.request;

/**
 * One row of utility meter data imported from a CSV/Excel file through the meter import flow.
 *
 * <p>Values are kept raw (strings) on purpose so that the same parsing + validation logic
 * used to build the preview is re-run server-side when the import is confirmed. No business
 * logic lives in this record.
 */
public record MeterImportRow(
        String houseId,
        String roomId,
        String utilityTypeId,
        String meterNumber,
        String installedAt,
        String status
) {
    public static MeterImportRow of(String houseId, String roomId, String utilityTypeId,
            String meterNumber, String installedAt, String status) {
        return new MeterImportRow(
                trimToNull(houseId),
                trimToNull(roomId),
                trimToNull(utilityTypeId),
                trimToNull(meterNumber),
                trimToNull(installedAt),
                trimToNull(status));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}