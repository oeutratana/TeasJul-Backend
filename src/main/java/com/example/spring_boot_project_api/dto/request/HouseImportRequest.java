package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record HouseImportRequest(
        @NotEmpty(message = "At least one import row is required")
        List<HouseImportRowRequest> rows
) {
}