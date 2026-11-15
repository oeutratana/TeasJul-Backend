package com.example.spring_boot_project_api.dto.request;

/**
 * One row of existing-house rental data entered through the CSV/Excel import flow.
 *
 * <p>Values are kept raw (strings) on purpose so that the same parsing + validation logic
 * used to build the preview is re-run server-side when the import is confirmed. No business
 * logic lives in this record.
 */
public record HouseImportRowRequest(
        String roomNo,
        String tenantName,
        String phone,
        String email,
        String moveInDate,
        String rent,
        String deposit,
        String contractStatus,
        Integer dueDay,
        String notes
) {
    public static HouseImportRowRequest of(String roomNo, String tenantName, String phone, String email,
            String moveInDate, String rent, String deposit, String contractStatus, Integer dueDay,
            String notes) {
        return new HouseImportRowRequest(
                trimToNull(roomNo),
                trimToNull(tenantName),
                trimToNull(phone),
                trimToNull(email),
                trimToNull(moveInDate),
                trimToNull(rent),
                trimToNull(deposit),
                trimToNull(contractStatus),
                dueDay,
                trimToNull(notes));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}