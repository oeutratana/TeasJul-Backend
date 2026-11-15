package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateRenewalRequest(
    @NotNull(message = "Contract ID is required")
    Long contractId,

    LocalDate previousEndDate,

    @NotNull(message = "New end date is required")
    LocalDate newEndDate,

    @NotNull(message = "Previous rent amount is required")
    @DecimalMin(value = "0.00", message = "Previous rent must be non-negative")
    BigDecimal previousRent,

    @NotNull(message = "New rent amount is required")
    @DecimalMin(value = "0.01", message = "New rent must be greater than zero")
    BigDecimal newRent,

    String notes,

    Long createdById
) {}
