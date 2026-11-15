package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateUtilityReadingRequest(
        @NotNull(message = "Meter is required")
        Long meterId,

        @NotNull(message = "Current reading is required")
        @DecimalMin(value = "0.0", message = "Current reading must not be negative")
        BigDecimal currentReading,

        @NotNull(message = "Reading date is required")
        LocalDate readingDate
) {
}
