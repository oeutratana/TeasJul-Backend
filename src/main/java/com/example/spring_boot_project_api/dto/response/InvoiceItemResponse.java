package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.InvoiceItemType;

public record InvoiceItemResponse(
        Long id,
        Long invoiceId,
        InvoiceItemType itemType,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
