package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.RoomImportRequest;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportResult;

/**
 * CSV/Excel import for rooms only (no tenants/contracts).
 * Follows the same preview-then-confirm pattern as the house onboarding import.
 */
public interface RoomImportService {

    /** Parses and validates an uploaded CSV/Excel file into a row-level preview. Nothing is written. */
    RoomImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content);

    RoomImportPreviewResponse previewImport(String fileName, byte[] content);

    /**
     * Confirms and transactionally imports previously previewed rows.
     * Every row is re-validated server-side before any room is written;
     * on any error nothing is persisted.
     */
    RoomImportResult importRooms(RoomImportRequest request);
}