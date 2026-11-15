package com.example.spring_boot_project_api.dto.response;

import java.util.List;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

/**
 * Single parsed + validated row returned by the preview endpoint. The raw fields are
 * returned as-is so the client can echo them back when confirming the import.
 */
public record HouseImportPreviewRowResponse(
        int rowNumber,
        String roomNo,
        String tenantName,
        String phone,
        String email,
        String moveInDate,
        String rent,
        String deposit,
        RentalContractStatus contractStatus,
        Integer dueDay,
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
    public static HouseImportPreviewRowResponse of(int rowNumber, String roomNo, String tenantName,
            String phone, String email, String moveInDate, String rent, String deposit,
            RentalContractStatus contractStatus, Integer dueDay, List<String> errors,
            List<String> warnings) {
        return new HouseImportPreviewRowResponse(rowNumber, roomNo, tenantName, phone, email,
                moveInDate, rent, deposit, contractStatus, dueDay, errors.isEmpty(), errors, warnings);
    }
}