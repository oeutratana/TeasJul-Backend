package com.example.spring_boot_project_api.enums;

/**
 * Publication workflow status for a room listing.
 *
 * <p>This is a SEPARATE concept from the room availability model
 * ({@link RoomStatus}). A room is only shown publicly when:
 * <pre>
 *     room.status        == RoomStatus.AVAILABLE
 * AND room.publicationStatus == PUBLISHED
 * </pre>
 * A room can be physically {@code AVAILABLE} while its listing is
 * {@code PENDING_APPROVAL} (available internally, not public yet).</p>
 */
public enum RoomPublicationStatus {
    DRAFT,
    PENDING_APPROVAL,
    PUBLISHED,
    REJECTED,
    UNPUBLISHED
}