package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;

import com.example.spring_boot_project_api.model.Gender;

/**
 * The owner's own identity as configured for rental agreements (Party A).
 * A component of {@link OwnerSettingsResponse}; never a separate settings system.
 */
public record OwnerContractInfoResponse(
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        String nationality,
        String nationalId,
        String currentAddress,
        String phone,
        String occupation
) {
}
