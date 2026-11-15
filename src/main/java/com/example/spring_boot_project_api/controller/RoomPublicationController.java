package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RejectRoomRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.service.RoomPublicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HTTP layer for the room listing publication workflow.
 *
 * <p>Authorization is enforced server-side in {@link RoomPublicationService}:
 * owners/managers submit the rooms they can access, while approve, reject and
 * unpublish are SUPER_ADMIN-only operations. The OWNER role is implicitly
 * granted every permission, so the admin gates rely on the explicit
 * {@code ROLE_SUPER_ADMIN} role check inside the service.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Room Publication",
        description = "Room listing publication workflow: owner submission and SUPER_ADMIN review.")
public class RoomPublicationController {

    private final RoomPublicationService roomPublicationService;
    private final RoomMapper roomMapper;

    @Operation(summary = "Submit a room for approval",
            description = "Owner or house manager submits a DRAFT or REJECTED room. On success the "
                    + "room moves to PENDING_APPROVAL.")
    @PostMapping("/api/rooms/{roomId}/submit")
    public ResponseEntity<RoomResponse> submitForApproval(@PathVariable Long roomId) {
        return ResponseEntity.ok(toResponse(roomPublicationService.submitForApproval(roomId)));
    }

    @Operation(summary = "List rooms by publication status",
            description = "Role-aware listing: OWNERs see only their own rooms, SUPER_ADMIN sees all. "
                    + "The status query parameter is required.")
    @GetMapping("/api/rooms/publication")
    public ResponseEntity<PageResponse<RoomResponse>> listRoomsByPublicationStatus(
            @RequestParam RoomPublicationStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(toPage(
                roomPublicationService.getRoomsByPublicationStatus(status, pageable)));
    }

    @Operation(summary = "Approve a pending room (SUPER_ADMIN only)",
            description = "PENDING_APPROVAL -> PUBLISHED. The room becomes visible on the public website.")
    @PostMapping("/api/admin/rooms/{roomId}/approve")
    public ResponseEntity<RoomResponse> approve(@PathVariable Long roomId) {
        return ResponseEntity.ok(toResponse(roomPublicationService.approve(roomId)));
    }

    @Operation(summary = "Reject a pending room (SUPER_ADMIN only)",
            description = "PENDING_APPROVAL -> REJECTED. A meaningful reason is required and is shown "
                    + "to the owner.")
    @PostMapping("/api/admin/rooms/{roomId}/reject")
    public ResponseEntity<RoomResponse> reject(@PathVariable Long roomId,
            @Valid @RequestBody RejectRoomRequest request) {
        return ResponseEntity.ok(
                toResponse(roomPublicationService.reject(roomId, request.reason())));
    }

    @Operation(summary = "Unpublish a published room (SUPER_ADMIN only)",
            description = "PUBLISHED -> UNPUBLISHED. The room is removed from the public website.")
    @PostMapping("/api/admin/rooms/{roomId}/unpublish")
    public ResponseEntity<RoomResponse> unpublish(@PathVariable Long roomId) {
        return ResponseEntity.ok(toResponse(roomPublicationService.unpublish(roomId)));
    }

    @Operation(summary = "List rooms for review (SUPER_ADMIN only)",
            description = "Paginated listing of rooms for the review workflow. When the status query "
                    + "parameter is omitted every room is returned regardless of publication status. "
                    + "An optional roomStatus parameter filters by the room availability status "
                    + "independent of the publication status (e.g. PUBLISHED + OCCUPIED is valid).")
    @GetMapping("/api/admin/rooms/publication")
    public ResponseEntity<PageResponse<RoomResponse>> listRoomsForReview(
            @RequestParam(required = false) RoomPublicationStatus status,
            @RequestParam(required = false) RoomStatus roomStatus,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                toPage(roomPublicationService.getRoomsForReview(status, roomStatus, pageable)));
    }

    private PageResponse<RoomResponse> toPage(Page<Room> page) {
        return PageResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private RoomResponse toResponse(Room room) {
        return roomMapper.toDto(room);
    }
}