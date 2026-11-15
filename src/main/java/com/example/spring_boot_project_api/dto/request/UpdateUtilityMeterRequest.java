package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.UtilityMeterStatus;

import jakarta.validation.constraints.Size;

public record UpdateUtilityMeterRequest(
        Long houseId,

        Long roomId,

        Long utilityTypeId,

        @Size(max = 100, message = "Meter number must not exceed 100 characters")
        String meterNumber,

        LocalDate installedAt,

        UtilityMeterStatus status
) {
}
