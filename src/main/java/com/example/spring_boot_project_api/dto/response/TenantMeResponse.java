package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OccupationType;
import com.example.spring_boot_project_api.model.TenantStatus;

public record TenantMeResponse(
        Long id,
        Long userId,
        Long ownerId,
        String ownerName,
        String ownerPhone,
        String ownerEmail,
        String tenantCode,
        String firstName,
        String lastName,
        Gender gender,
        LocalDate dateOfBirth,
        String phone,
        String email,
        OccupationType occupationType,
        String occupation,
        String schoolOrCompany,
        String idType,
        String idNumber,
        String address,
        TenantStatus status,
        TenantMeHouse house,
        TenantMeRoom room,
        TenantMeContract contract,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}