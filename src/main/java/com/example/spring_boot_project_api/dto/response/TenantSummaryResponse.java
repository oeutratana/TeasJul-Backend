package com.example.spring_boot_project_api.dto.response;

import java.util.Map;

/**
 * Aggregate view of the current user's tenants (all statuses, computed in the
 * database). Used to keep dashboard stat cards correct while the tenant list
 * itself is paginated server-side.
 */
public record TenantSummaryResponse(
        long total,
        Map<String, Long> statusCounts
) {
}