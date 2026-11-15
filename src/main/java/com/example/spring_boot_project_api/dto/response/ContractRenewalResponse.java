package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ContractRenewalResponse(
    Long id,
    Long contractId,
    LocalDate previousEndDate,
    LocalDate newEndDate,
    BigDecimal previousRent,
    BigDecimal newRent,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime renewedAt,
    
    String notes,
    Long createdById,
    String createdByName
) {}
