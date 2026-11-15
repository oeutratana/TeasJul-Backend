package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;

public record TenantInvoiceDetailResponse(
        Long id,
        String invoiceNo,
        String billingMonth,
        String houseName,
        String roomNumber,
        BigDecimal rent,
        List<UtilityDetail> utilities,
        BigDecimal otherCharges,
        BigDecimal total,
        InvoiceStatus status,
        LocalDate dueDate
) {

    public record UtilityDetail(
            InvoiceItemType type,
            BigDecimal usage,
            String unit,
            BigDecimal amount
    ) {
    }
}