package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Bulk "Save All" payload for the monthly meter-reading sheet. A single
 * readingDate (nullable) applies to every row; when omitted, the last day of
 * the billing month is used.
 */
public record MonthlyReadingBulkRequest(

        @NotNull(message = "House is required")
        Long houseId,

        @NotNull(message = "Billing year is required")
        @Min(value = 2000, message = "Billing year must be at least 2000")
        Integer billingYear,

        @NotNull(message = "Billing month is required")
        @Min(value = 1, message = "Billing month must be between 1 and 12")
        @Max(value = 12, message = "Billing month must be between 1 and 12")
        Integer billingMonth,

        LocalDate readingDate,

        @NotEmpty(message = "At least one meter reading is required")
        @Valid
        List<MonthlyReadingRowRequest> rows
) {

    public record MonthlyReadingRowRequest(
            @NotNull(message = "Meter is required")
            Long meterId,

            @NotNull(message = "Current reading is required")
            @DecimalMin(value = "0.0", message = "Current reading must not be negative")
            BigDecimal currentReading
    ) {
    }
}