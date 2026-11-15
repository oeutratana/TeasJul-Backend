package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.HouseImportRequest;
import com.example.spring_boot_project_api.dto.request.HouseImportRowRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupPreviewRequest;
import com.example.spring_boot_project_api.dto.response.HouseImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.HouseImportResult;
import com.example.spring_boot_project_api.dto.response.TenantResponse;

/**
 * Onboarding for an existing house that already has rental data outside the system.
 *
 * <p>The New/Existing choice is a one-time setup decision only. Once the data is imported the
 * house behaves like any other: Room, Tenant, RentalContract and ContractTenant are created
 * through the normal relationships (never a duplicate tenant-room system, never a
 * {@code rooms.tenant_id}).
 *
 * <p>Authorization is derived from the authenticated user (owner, or manager assigned via
 * {@code user_house_access}). An {@code ownerId} from the client is never trusted.
 */
public interface HouseOnboardingService {

    /** Parses a CSV/Excel upload and validates every row, producing a row-level preview. */
    HouseImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content);

    /** Validates the uploaded file and returns the parsed rows (used to build the preview). */
    List<HouseImportRowRequest> parseImport(Long houseId, byte[] content);

    /**
     * Confirms and transactionally imports previously previewed rows into the given house.
     * Re-validates server-side, so rows must still be valid.
     */
    HouseImportResult importExisting(Long houseId, HouseImportRequest request);

    /** Lightweight listing of tenants belonging to the house owner (for manual setup UX). */
    List<TenantResponse> getHouseOwnerTenants(Long houseId);

    /**
     * Validates manually entered rows for existing house setup.
     * Returns row-level preview with errors/warnings. Nothing is written.
     */
    HouseImportPreviewResponse previewManualSetup(Long houseId, HouseManualSetupPreviewRequest request);

    /**
     * Transactionally creates rooms, tenants, contracts and contract-tenants
     * from manually entered data for an existing house.
     */
    HouseImportResult manualSetup(Long houseId, HouseManualSetupRequest request);
}