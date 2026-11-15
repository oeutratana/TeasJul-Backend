package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregate view of the current user's rental contracts, computed in the
 * database. Backs the stat cards on the contracts page while the contract list
 * is paginated server-side.
 */
public record ContractSummaryResponse(
        long total,
        Map<String, Long> statusCounts,
        BigDecimal activeMonthlyRent,
        List<ExpiringContractSummary> expiringSoon
) {
}