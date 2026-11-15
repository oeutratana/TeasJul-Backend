package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record PropertyOwnerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}