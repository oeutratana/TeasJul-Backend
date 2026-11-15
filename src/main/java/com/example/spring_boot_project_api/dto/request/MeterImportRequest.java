package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record MeterImportRequest(
        Long houseId,
        @NotEmpty(message = "At least one import row is required")
        List<MeterImportRow> rows
) {
    public MeterImportRequest(List<MeterImportRow> rows) {
        this(null, rows);
    }
}