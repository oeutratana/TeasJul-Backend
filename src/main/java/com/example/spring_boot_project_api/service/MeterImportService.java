package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.MeterImportRequest;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportResult;

/**
 * CSV/Excel import for utility meters.
 * Follows the same preview-then-confirm pattern as the room and house onboarding imports.
 */
public interface MeterImportService {

    /** Parses and validates an uploaded CSV/Excel file into a row-level preview. Nothing is written. */
    MeterImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content);

    MeterImportPreviewResponse previewImport(String fileName, byte[] content);

    /**
     * Confirms and transactionally imports previously previewed rows.
     * Every row is re-validated server-side before any meter is written;
     * on any error nothing is persisted.
     */
    MeterImportResult importMeters(MeterImportRequest request);
}