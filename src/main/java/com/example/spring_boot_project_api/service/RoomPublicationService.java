package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Room;

/**
 * Domain service for the room listing publication workflow (Phase 3).
 *
 * <p>It drives the {@code rooms.publication_status} state machine while the
 * existing {@code rooms.status} caretaker model keeps governing room
 * availability. The two are intentionally decoupled:</p>
 *
 * <ul>
 *   <li><b>Owners / house managers</b> submit rooms they own or have house
 *       access to (governed by {@link HouseAccessService}) for approval.</li>
 *   <li><b>SUPER_ADMIN</b> only can approve, reject or unpublish a room. The
 *       OWNER role is implicitly granted every permission, so the admin gates
 *       here are enforced by role ({@code ROLE_SUPER_ADMIN}), never by the
 *       generic permission check alone.</li>
 * </ul>
 *
 * <p>Transitions follow the approved spec exactly:</p>
 * <pre>
 *   DRAFT      -> PENDING_APPROVAL   (owner submit)
 *   REJECTED   -> PENDING_APPROVAL   (owner resubmit)
 *   PENDING_APPROVAL -> PUBLISHED    (SUPER_ADMIN approve)
 *   PENDING_APPROVAL -> REJECTED     (SUPER_ADMIN reject, reason required)
 *   PUBLISHED  -> UNPUBLISHED        (SUPER_ADMIN unpublish)
 * </pre>
 * Any other transition throws {@link IllegalStateException}.
 */
public interface RoomPublicationService {

    Room submitForApproval(Long roomId);

    Room approve(Long roomId);

    Room reject(Long roomId, String rejectionReason);

    Room unpublish(Long roomId);

    RoomPublicationStatus getPublicationStatus(Long roomId);

    Page<Room> getPendingApprovalRooms(Pageable pageable);

    Page<Room> getPublishedAvailableRooms(Pageable pageable);

    Page<Room> getRoomsByPublicationStatus(RoomPublicationStatus publicationStatus, Pageable pageable);

    /**
     * SUPER_ADMIN-only paged listing of rooms for the review workflow.
     *
     * @param publicationStatus optional publication status filter; when
     *        {@code null} every room is returned regardless of publication status
     * @param roomStatus optional room availability filter; when {@code null}
     *        rooms of every availability status are returned. The two statuses
     *        are independent: {@code PUBLISHED + OCCUPIED} is valid and does
     *        not imply the listing should be unpublished.
     */
    Page<Room> getRoomsForReview(RoomPublicationStatus publicationStatus,
            RoomStatus roomStatus, Pageable pageable);
}
