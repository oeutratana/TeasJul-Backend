package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

public record TenantPaymentHistoryResponse(
        String month,
        BigDecimal amount,
        InvoiceStatus status
) {
}