package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceCommentRequest(

        @NotBlank(message = "Comment is required")
        @Size(max = 65535, message = "Comment must not exceed 65535 characters")
        String comment
) {
}
