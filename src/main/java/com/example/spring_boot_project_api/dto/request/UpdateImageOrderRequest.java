package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateImageOrderRequest(
        @NotNull(message = "sortOrder is required") Integer sortOrder
) {
}