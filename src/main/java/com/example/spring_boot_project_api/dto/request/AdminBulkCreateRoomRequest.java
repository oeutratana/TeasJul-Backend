package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * SUPER_ADMIN request to create several physical rooms for one OWNER in a single
 * call. Every entry references the same owner's house and room type; the admin
 * service verifies ownership before anything is persisted. When
 * {@code publishImmediately} is true every created room is published and
 * approved by the current admin, otherwise they are all created as DRAFT.
 */
@Data
public class AdminBulkCreateRoomRequest {

    /** When true every room is created and immediately published as approved by the admin. */
    private Boolean publishImmediately = false;

    @NotEmpty(message = "At least one room is required")
    @Valid
    private List<AdminCreateRoomRequest> rooms;
}