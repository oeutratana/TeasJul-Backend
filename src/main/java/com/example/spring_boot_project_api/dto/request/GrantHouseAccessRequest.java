package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GrantHouseAccessRequest(

        @NotNull(message = "User id is required")
        @Positive(message = "User id must be a positive number")
        Long userId
) {
}
