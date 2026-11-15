package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OccupationType;
import com.example.spring_boot_project_api.model.TenantStatus;

public record TenantResponse(
        Long id,
        Long userId,
        Long ownerId,
        Long houseId,
        Long invitedById,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
