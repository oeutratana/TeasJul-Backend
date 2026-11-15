package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A contract that is eligible for invoice generation in the current user's
 * accessible houses. Only ACTIVE contracts whose room belongs to an accessible
 * house are returned, so rooms without an active contract are never listed.
 */
public record ContractEligibleResponse(
        Long contractId,
        String contractNo,
        Long tenantId,
        String tenantName,
        Long roomId,
        String roomNumber,
        Long houseId,
        String houseName,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate,
        boolean alreadyInvoiced
) {
}
