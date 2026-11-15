package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

public record InvoiceResponse(
        Long id,
        Long ownerId,
        String invoiceNo,
        Long contractId,
        String contractNo,
        Long tenantId,
        String tenantName,
        Long roomId,
        String roomNumber,
        Long houseId,
        String houseName,
        Integer billingYear,
        Integer billingMonth,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal lateFee,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        InvoiceStatus status,
        BigDecimal exchangeRate,
        String exchangeRateCurrency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InvoiceItemResponse> items
) {
}
