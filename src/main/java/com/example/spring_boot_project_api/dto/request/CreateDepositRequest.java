package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.ContractDepositStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateDepositRequest(
    @NotNull(message = "Contract ID is required")
    Long contractId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    String notes,

    ContractDepositStatus status,

    LocalDate paidAt
) {}
