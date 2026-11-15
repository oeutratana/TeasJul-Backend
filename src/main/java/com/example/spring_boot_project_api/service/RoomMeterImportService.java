package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.RoomMeterImportRequest;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportResult;

/**
 * CSV/Excel import that creates rooms and, optionally, utility meters attached to them.
 *
 * <p>A standalone flow distinct from the room-only import ({@link RoomImportService}) and the
 * meter-only import ({@link MeterImportService}). The house, room type and optional meter
 * install date are supplied once (from the UI) and apply to every row; meters are attached to
 * the persisted room id, never by room number.
 */
public interface RoomMeterImportService {

    /**
     * Parses and validates an uploaded CSV/Excel file into a row-level preview using the
     * UI-supplied house and room type context. Nothing is written. {@code installDate} is an
     * optional ISO date string; it is validated when present.
     */
    RoomMeterImportPreviewResponse previewImport(Long houseId, Long roomTypeId, String installDate,
            String fileName, byte[] content);

    /**
     * Confirms and transactionally imports previously previewed rows: rooms first, then meters
     * attached to the created room ids. Every row is re-validated server-side before anything
     * is written; on any error nothing is persisted.
     */
    RoomMeterImportResult importRoomsWithMeters(RoomMeterImportRequest request);
}