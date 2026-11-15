package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OccupationType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        Gender gender,

        LocalDate dateOfBirth,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        OccupationType occupationType,

        @Size(max = 150, message = "Occupation must not exceed 150 characters")
        String occupation,

        @Size(max = 200, message = "School or company must not exceed 200 characters")
        String schoolOrCompany,

        @Size(max = 50, message = "Id type must not exceed 50 characters")
        String idType,

        @Size(max = 100, message = "Id number must not exceed 100 characters")
        String idNumber,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
