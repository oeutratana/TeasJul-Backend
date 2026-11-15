package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import com.example.spring_boot_project_api.model.Gender;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * The owner's own identity as it must appear on rental agreements (Party A).
 *
 * <p>This is a component of {@link UpdateOwnerSettingsRequest} - it is edited and
 * persisted through the existing {@code GET/PUT /api/v1/settings} endpoints, not
 * through a separate API surface. Every field is optional: a blank value is stored
 * as a blank value and the contract simply omits the row.</p>
 */
public record UpdateOwnerContractInfoRequest(

        @Size(max = 200, message = "Full name must not exceed 200 characters")
        String fullName,

        Gender gender,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 100, message = "Nationality must not exceed 100 characters")
        String nationality,

        @Size(max = 100, message = "National ID must not exceed 100 characters")
        String nationalId,

        @Size(max = 255, message = "Current address must not exceed 255 characters")
        String currentAddress,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 150, message = "Occupation must not exceed 150 characters")
        String occupation
) {
}
