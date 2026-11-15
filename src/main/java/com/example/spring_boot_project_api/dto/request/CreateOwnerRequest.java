package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Super admin creates an OWNER account.
 *
 * <p>There is deliberately <b>no role field</b>: the account always receives the
 * existing OWNER role, decided server-side. A client-forged {@code role} field
 * (for example {@code ADMIN} or {@code SUPER_ADMIN}) has no DTO slot to bind to
 * and is therefore ignored by Jackson, so role escalation is structurally
 * impossible.</p>
 */
public record CreateOwnerRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @Size(max = 30)
        String phone
) {
}