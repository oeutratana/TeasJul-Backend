package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Owner-supplied final late fee for an unpaid invoice. The amount is stored on
 * {@code invoices.late_fee}; a value of zero waives the fee.
 */
public record UpdateInvoiceLateFeeRequest(
        @NotNull(message = "Late fee is required")
        @DecimalMin(value = "0.00", message = "Late fee must not be negative")
        @Digits(integer = 10, fraction = 2, message = "Late fee must have at most 2 decimal places")
        BigDecimal lateFee
) {
}
