package com.example.spring_boot_project_api.dto.response;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import java.time.LocalDateTime;

public record AmenityResponse(
    Long id,
    Long ownerId,
    String name,
    String code,
    String description,
    String icon,
    AmenityStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}