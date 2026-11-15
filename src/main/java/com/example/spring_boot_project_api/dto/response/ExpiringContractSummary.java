package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;

/**
 * Lightweight projection of an active contract that is close to its end date.
 * Deliberately omits heavy fields (room, tenant names) because it is only used
 * to render the expiring-soon banner on the contracts page.
 */
public record ExpiringContractSummary(
        Long id,
        String contractNo,
        LocalDate endDate
) {
}