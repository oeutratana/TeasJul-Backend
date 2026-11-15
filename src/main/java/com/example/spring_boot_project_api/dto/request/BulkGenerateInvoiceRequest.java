package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BulkGenerateInvoiceRequest(
        @NotNull(message = "Billing year is required")
        @Min(value = 2000, message = "Billing year must be at least 2000")
        Integer billingYear,

        @NotNull(message = "Billing month is required")
        @Min(value = 1, message = "Billing month must be between 1 and 12")
        @Max(value = 12, message = "Billing month must be between 1 and 12")
        Integer billingMonth,

        @DecimalMin(value = "0.0", message = "Discount must not be negative")
        BigDecimal discount,

        // Optional: overrides the due date derived from each contract's dueDay.
        LocalDate dueDate,

        // Optional: restricts generation to a single accessible house.
        // When omitted, every house the current user can access is used.
        Long houseId
) {
}
