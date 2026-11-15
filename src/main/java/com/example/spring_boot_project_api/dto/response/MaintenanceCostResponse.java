package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.MaintenanceCostType;

public record MaintenanceCostResponse(
        Long id,
        Long requestId,
        MaintenanceCostType costType,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
