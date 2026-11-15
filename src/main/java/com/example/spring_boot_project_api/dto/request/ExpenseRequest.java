package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.ExpenseStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
    @NotNull(message = "Owner ID is required")
    Long ownerId,

    @NotBlank(message = "Expense number is required")
    @Size(max = 50, message = "Expense number cannot exceed 50 characters")
    String expenseNo,

    Long houseId,

    @NotNull(message = "Category ID is required")
    Long categoryId,

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotNull(message = "Expense date is required")
    LocalDate expenseDate,

    // Long paymentMethodId,

    ExpenseStatus status,

    Long createdById
) {}
