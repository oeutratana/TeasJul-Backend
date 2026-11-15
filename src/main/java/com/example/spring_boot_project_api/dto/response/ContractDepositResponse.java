package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ContractDepositResponse(
    Long id,
    Long contractId,
    BigDecimal amount,
    BigDecimal paidAmount,
    ContractDepositStatus status,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime paidAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime refundedAt,
    
    String notes,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt
) {}
