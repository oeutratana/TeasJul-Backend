package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for SUPER_ADMIN rejection of a room submission.
 *
 * <p>The reason is required and bounded to the same limit enforced by
 * {@code rooms.rejection_reason} ({@code 1000} characters).</p>
 */
public record RejectRoomRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
        String reason
) {
}