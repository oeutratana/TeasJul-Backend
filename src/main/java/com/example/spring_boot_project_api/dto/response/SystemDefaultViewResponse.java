package com.example.spring_boot_project_api.dto.response;

import com.example.spring_boot_project_api.enums.OwnerSystemDefaultStatus;

/**
 * Owner-facing view of a system default. Adds the current owner's
 * assignment state (enabled/disabled) and the id of the owner-scoped
 * UtilityType/Amenity materialised for that default (if any).
 */
public record SystemDefaultViewResponse(
        SystemDefaultResponse definition,
        boolean enabled,
        OwnerSystemDefaultStatus assignmentStatus,
        Long linkedId
) {
}