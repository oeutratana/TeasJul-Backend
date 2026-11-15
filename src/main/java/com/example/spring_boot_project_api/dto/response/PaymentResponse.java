package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        Long id,
        Long invoiceId,
        Long paymentMethodId,
        String paymentMethodName,
        String paymentNo,
        String transactionId,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        String exchangeRateTo,
        PaymentStatus status,
        String qrCode,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
