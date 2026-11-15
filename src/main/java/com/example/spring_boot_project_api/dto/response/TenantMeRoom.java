package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

public record TenantMeRoom(
        Long id,
        String roomNumber,
        String name,
        Integer floorNumber,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit
) {
}