package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UtilityReadingResponse(
        Long id,
        Long meterId,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal usage,
        LocalDate readingDate,
        LocalDateTime createdAt
) {
}
