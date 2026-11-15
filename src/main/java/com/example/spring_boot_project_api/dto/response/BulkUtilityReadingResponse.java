package com.example.spring_boot_project_api.dto.response;

public record BulkUtilityReadingResponse(
        int createdCount,
        int updatedCount,
        int skippedCount
) {
}