package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.UtilityRateStatus;

public record UtilityRateResponse(
        Long id,
        Long ownerId,
        Long houseId,
        Long utilityTypeId,
        BigDecimal rate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        UtilityRateStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
