package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Code is required")
        @Size(max = 100, message = "Code must not exceed 100 characters")
        String code,

        @NotBlank(message = "Module is required")
        @Size(max = 100, message = "Module must not exceed 100 characters")
        String module,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
