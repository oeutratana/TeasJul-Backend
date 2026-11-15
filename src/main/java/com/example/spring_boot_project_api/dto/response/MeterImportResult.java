package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record MeterImportResult(
        int metersCreated,
        List<String> messages
) {
    public static MeterImportResult of(int metersCreated, List<String> messages) {
        return new MeterImportResult(metersCreated, messages);
    }
}