package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RoomImportRequest(
        Long houseId,
        @NotEmpty(message = "At least one import row is required")
        List<RoomImportRow> rows
) {
    public RoomImportRequest(List<RoomImportRow> rows) {
        this(null, rows);
    }
}