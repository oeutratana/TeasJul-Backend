package com.example.spring_boot_project_api.dto.response;

import java.util.Map;

/**
 * Aggregate view of all users by status, computed in the database. Kept
 * separate from the paginated user list so admin stat cards stay accurate.
 */
public record UserSummaryResponse(
        long total,
        Map<String, Long> statusCounts
) {
}