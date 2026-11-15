package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Confirmation payload for the rooms + meters import. The house, room type and optional meter
 * install date are authoritative and apply to every row in the file; they are chosen in the UI,
 * not parsed from the CSV.
 */
public record RoomMeterImportRequest(
        Long houseId,
        Long roomTypeId,
        LocalDate installDate,
        @NotEmpty(message = "At least one import row is required")
        List<RoomMeterImportRow> rows
) {
    public RoomMeterImportRequest(List<RoomMeterImportRow> rows) {
        this(null, null, null, rows);
    }
}