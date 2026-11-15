package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.UtilityMeterStatus;

public record UtilityMeterResponse(
        Long id,
        Long ownerId,
        Long houseId,
        Long roomId,
        Long utilityTypeId,
        String meterNumber,
        LocalDate installedAt,
        UtilityMeterStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
