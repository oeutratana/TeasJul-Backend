package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.UtilityRateStatus;

import jakarta.validation.constraints.DecimalMin;

public record UpdateUtilityRateRequest(
        Long houseId,

        Long utilityTypeId,

        @DecimalMin(value = "0.0", message = "Rate must not be negative")
        BigDecimal rate,

        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        UtilityRateStatus status
) {
}
