package com.example.spring_boot_project_api.dto.response;

import java.util.List;

/**
 * Summary of a bulk room assignment operation (bulk room utilities / bulk room amenities).
 *
 * @param assignmentsCreated number of assignments actually created (or reactivated)
 * @param alreadyAssigned    number of (room, entity) pairs already assigned and skipped
 * @param failures           per-pair entries that could not be assigned, with a reason
 */
public record BulkAssignmentResponse(
        int roomsProcessed,
        int assignmentsCreated,
        int alreadyAssigned,
        List<Failure> failures
) {
    public static BulkAssignmentResponse of(int roomsProcessed, int assignmentsCreated,
            int alreadyAssigned, List<Failure> failures) {
        return new BulkAssignmentResponse(roomsProcessed, assignmentsCreated, alreadyAssigned, failures);
    }

    public record Failure(
            Long roomId,
            Long entityId,
            String reason
    ) {
        public static Failure of(Long roomId, Long entityId, String reason) {
            return new Failure(roomId, entityId, reason);
        }
    }
}