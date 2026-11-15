package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

public record TenantMeContract(
        Long id,
        String contractNo,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        Integer dueDay,
        BigDecimal depositPaidAmount,
        Boolean depositFullyPaid,
        RentalContractStatus status
) {
}