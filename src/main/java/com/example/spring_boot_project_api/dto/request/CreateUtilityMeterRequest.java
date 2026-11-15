package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.UtilityMeterStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUtilityMeterRequest(
        @NotNull(message = "House is required")
        Long houseId,

        Long roomId,

        @NotNull(message = "Utility type is required")
        Long utilityTypeId,

        @NotBlank(message = "Meter number is required")
        @Size(max = 100, message = "Meter number must not exceed 100 characters")
        String meterNumber,

        LocalDate installedAt,

        UtilityMeterStatus status
) {
}
