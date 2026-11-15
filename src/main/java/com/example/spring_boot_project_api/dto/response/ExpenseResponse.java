package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.ExpenseStatus;

public record ExpenseResponse(
    Long id,
    Long ownerId,
    String expenseNo,
    Long houseId,
    String houseName,
    Long categoryId,
    String categoryName,
    String description,
    BigDecimal amount,
    LocalDate expenseDate,
    // Long paymentMethodId,
    // String paymentMethodName,
    ExpenseStatus status,
    Long createdById,
    String createdByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
