package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contact/ownership information for a property owner that does not have a
 * TeasJul login account. No credentials are ever created from this request.
 */
public record PropertyOwnerRequest(

        @NotBlank(message = "Owner name is required")
        @Size(max = 200, message = "Owner name must not exceed 200 characters")
        String name,

        @Size(max = 320, message = "Owner email must not exceed 320 characters")
        String email,

        @Size(max = 50, message = "Owner phone must not exceed 50 characters")
        String phone,

        @Size(max = 500, message = "Owner address must not exceed 500 characters")
        String address,

        String notes
) {
}