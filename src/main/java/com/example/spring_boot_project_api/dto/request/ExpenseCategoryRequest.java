package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.ExpenseCategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseCategoryRequest(
    @NotNull(message = "Owner ID is required")
    Long ownerId,

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @NotNull(message = "Status is required")
    ExpenseCategoryStatus status
) {}
