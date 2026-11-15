package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateDepositPaymentRequest(
    @NotNull(message = "Paid amount is required")
    @DecimalMin
    (value = "0.01", message = "Paid amount must be greater than zero")
    BigDecimal paidAmount

) {}
