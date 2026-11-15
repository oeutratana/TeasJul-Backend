package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.RoomPublicationService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Default implementation of {@link RoomPublicationService}.
 *
 * <p>Access rules (resolved server-side, never trusted from the client):</p>
 * <ul>
 *   <li>Submitting a room for approval requires the current user to be the
 *       room's owner, or a manager acting on a room whose house the current
 *       user manages.</li>
 *   <li>Approving, rejecting and unpublishing are SUPER_ADMIN operations only,
 *       guarded by an explicit role check (never the generic permission check,
 *       because OWNER is implicitly granted every permission).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RoomPublicationServiceImpl implements RoomPublicationService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final RoomRepository roomRepository;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Room submitForApproval(Long roomId) {
        Room room = getAccessibleRoom(roomId);
        if (room.getPublicationStatus() != RoomPublicationStatus.DRAFT
                && room.getPublicationStatus() != RoomPublicationStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only DRAFT or REJECTED rooms can be submitted for approval");
        }

        room.setPublicationStatus(RoomPublicationStatus.PENDING_APPROVAL);
        room.setSubmittedAt(LocalDateTime.now());
        room.setSubmittedBy(securityUtils.getCurrentUserId());
        Room submitted = roomRepository.save(room);
        notificationService.notifyPropertySubmitted(submitted);
        return submitted;
    }

    @Override
    @Transactional
    public Room approve(Long roomId) {
        requireSuperAdmin();
        Room room = getRoom(roomId);
        boolean directAdminPublish = securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
            && room.getPublicationStatus() == RoomPublicationStatus.DRAFT;
        if (room.getPublicationStatus() != RoomPublicationStatus.PENDING_APPROVAL
            && !directAdminPublish) {
            throw new IllegalStateException(
                "Only PENDING_APPROVAL rooms can be approved, or DRAFT rooms can be published by a SUPER_ADMIN");
        }

        room.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        room.setApprovedAt(LocalDateTime.now());
        room.setApprovedBy(securityUtils.getCurrentUserId());
        room.setPublishedAt(LocalDateTime.now());
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room reject(Long roomId, String rejectionReason) {
        requireSuperAdmin();
        Room room = getRoom(roomId);
        if (room.getPublicationStatus() != RoomPublicationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL rooms can be rejected");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (rejectionReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Rejection reason cannot exceed 1000 characters");
        }

        room.setPublicationStatus(RoomPublicationStatus.REJECTED);
        room.setRejectedAt(LocalDateTime.now());
        room.setRejectedBy(securityUtils.getCurrentUserId());
        room.setRejectionReason(rejectionReason.trim());
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room unpublish(Long roomId) {
        requireSuperAdmin();
        Room room = getRoom(roomId);
        if (room.getPublicationStatus() != RoomPublicationStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED rooms can be unpublished");
        }

        room.setPublicationStatus(RoomPublicationStatus.UNPUBLISHED);
        return roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomPublicationStatus getPublicationStatus(Long roomId) {
        return getAccessibleRoom(roomId).getPublicationStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Room> getPendingApprovalRooms(Pageable pageable) {
        requireSuperAdmin();
        return roomRepository.findByPublicationStatus(
                RoomPublicationStatus.PENDING_APPROVAL,
                sortedBySubmittedAtAsc(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Room> getPublishedAvailableRooms(Pageable pageable) {
        return roomRepository.findByPublicationStatusAndStatus(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE,
                sortedByPublishedAtDesc(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Room> getRoomsByPublicationStatus(RoomPublicationStatus publicationStatus,
            Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return roomRepository.findByPublicationStatus(
                    publicationStatus, sortedBySubmittedAtAsc(pageable));
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return roomRepository.findByOwnerIdAndPublicationStatus(
                    currentUserId, publicationStatus, sortedBySubmittedAtAsc(pageable));
        }
        throw new AccessDeniedException(ACCESS_DENIED_MSG);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Room> getRoomsForReview(RoomPublicationStatus publicationStatus,
            RoomStatus roomStatus, Pageable pageable) {
        requireSuperAdmin();
        return roomRepository.findRoomsForReview(
                publicationStatus, roomStatus, sortedBySubmittedAtAsc(pageable));
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    /**
     * Owner or manager access to the room. The current user must be the room's
     * owner, or a manager whose managed houses include the room owner's house.
     */
    private Room getAccessibleRoom(Long roomId) {
        Room room = getRoom(roomId);
        Long currentUserId = securityUtils.getCurrentUserId();
        if (room.getOwnerId() != null && room.getOwnerId().equals(currentUserId)) {
            return room;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && room.getHouseId() != null
                && houseAccessService.canManageHouse(room.getHouseId())) {
            return room;
        }
        throw new AccessDeniedException(ACCESS_DENIED_MSG);
    }

    private Pageable sortedBySubmittedAtAsc(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id")));
    }

    private Pageable sortedByPublishedAtDesc(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("id")));
    }
}
