package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentStatusResponse(
        String transactionId,
        Long paymentId,
        Long invoiceId,
        String invoiceNo,
        PaymentStatus paymentStatus,
        InvoiceStatus invoiceStatus,
        BigDecimal amount,
        String currency,
        LocalDateTime paidAt
) {
}