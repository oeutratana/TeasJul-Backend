package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentListResponse(
        Long id,
        String paymentNo,
        String transactionId,
        Long invoiceId,
        String invoiceNo,
        Long paymentMethodId,
        String paymentMethodName,
        Long houseId,
        String houseName,
        Long tenantId,
        String tenantName,
        String roomNumber,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}