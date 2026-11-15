package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Assigns one or more utility types to many rooms in a single operation.
 *
 * <p>The same {@code isIncluded} flag is applied to every created assignment. Each
 * (room, utility type) pair is deduplicated; existing ACTIVE assignments are reported
 * back in the response summary instead of being duplicated.
 */
public record BulkRoomUtilityRequest(
        @NotEmpty(message = "At least one room is required")
        List<Long> roomIds,

        @NotEmpty(message = "At least one utility type is required")
        List<Long> utilityTypeIds,

        Boolean isIncluded
) {
}