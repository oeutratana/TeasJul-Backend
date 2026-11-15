package com.example.spring_boot_project_api.dto.response;

import com.example.spring_boot_project_api.enums.ExpenseCategoryStatus;
import java.time.LocalDateTime;

public record ExpenseCategoryResponse(
    Long id,
    Long ownerId,
    String name,
    String code,
    String description,
    ExpenseCategoryStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
