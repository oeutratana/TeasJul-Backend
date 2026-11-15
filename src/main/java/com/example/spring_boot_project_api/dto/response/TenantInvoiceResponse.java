package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

public record TenantInvoiceResponse(
        Long id,
        String invoiceNo,
        String billingMonth,
        BigDecimal rent,
        BigDecimal water,
        BigDecimal electricity,
        BigDecimal otherCharges,
        BigDecimal total,
        InvoiceStatus status,
        LocalDate dueDate
) {
}