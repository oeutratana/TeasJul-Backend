package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.UtilityRateStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateUtilityRateRequest(
        @NotNull(message = "House is required")
        Long houseId,

        @NotNull(message = "Utility type is required")
        Long utilityTypeId,

        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.0", message = "Rate must not be negative")
        BigDecimal rate,

        @NotNull(message = "Effective from is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        UtilityRateStatus status
) {
}
