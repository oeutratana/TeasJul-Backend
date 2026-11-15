package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoicePreviewResponse(
        Long contractId,
        String contractNo,
        Long roomId,
        String roomNumber,
        Long houseId,
        String houseName,
        Long tenantId,
        String tenantName,
        Integer billingYear,
        Integer billingMonth,
        LocalDate dueDate,
        List<InvoiceItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
) {
}
