package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomPublicationServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HouseAccessService houseAccessService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RoomPublicationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(20L);
    }

    private Room room(Long ownerId, RoomPublicationStatus publicationStatus) {
        Room room = new Room();
        room.setId(1L);
        room.setOwnerId(ownerId);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPublicationStatus(publicationStatus);
        return room;
    }

    private void adminRole() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    private void ownerRole() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    // ---------- submitForApproval ----------

    @Test
    void submitForApproval_byOwner_movesDraftToPendingApproval() {
        ownerRole();
        Room room = room(20L, RoomPublicationStatus.DRAFT);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.submitForApproval(1L);

        assertThat(result.getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.PENDING_APPROVAL);
        assertThat(result.getSubmittedAt()).isNotNull();
        assertThat(result.getSubmittedBy()).isEqualTo(20L);
        verify(notificationService).notifyPropertySubmitted(room);
    }

    @Test
    void submitForApproval_byOwner_resubmitsRejectedRoomAndNotifies() {
        ownerRole();
        Room room = room(20L, RoomPublicationStatus.REJECTED);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.submitForApproval(1L);

        assertThat(result.getPublicationStatus()).isEqualTo(RoomPublicationStatus.PENDING_APPROVAL);
        verify(notificationService).notifyPropertySubmitted(room);
    }

    @Test
    void submitForApproval_doesNotNotifyWhenAlreadyPending() {
        ownerRole();
        Room room = room(20L, RoomPublicationStatus.PENDING_APPROVAL);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.submitForApproval(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(notificationService, never()).notifyPropertySubmitted(any(Room.class));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void submitForApproval_whenNotOwner_throws() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        Room room = room(999L, RoomPublicationStatus.DRAFT);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.submitForApproval(1L))
                .isInstanceOf(RuntimeException.class);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void submitForApproval_whenRoomMissing_throwsNotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitForApproval(1L))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void submitForApproval_byManagerWithHouseAccess_movesDraftToPendingApproval() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        Room room = room(999L, RoomPublicationStatus.DRAFT);
        room.setHouseId(7L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(houseAccessService.canManageHouse(7L)).thenReturn(true);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.submitForApproval(1L);

        assertThat(result.getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.PENDING_APPROVAL);
        verify(notificationService).notifyPropertySubmitted(room);
    }

    @Test
    void submitForApproval_byManagerWithoutHouseAccess_isDenied() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        Room room = room(999L, RoomPublicationStatus.DRAFT);
        room.setHouseId(7L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(houseAccessService.canManageHouse(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.submitForApproval(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomRepository, never()).save(any(Room.class));
        verify(notificationService, never()).notifyPropertySubmitted(any(Room.class));
    }

    // ---------- approve ----------

    @Test
    void approve_bySuperAdmin_movesToPublished() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.PENDING_APPROVAL);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.approve(1L);

        assertThat(result.getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.PUBLISHED);
        assertThat(result.getApprovedAt()).isNotNull();
        assertThat(result.getApprovedBy()).isEqualTo(20L);
        assertThat(result.getPublishedAt()).isNotNull();
    }

    @Test
    void approve_whenNotSuperAdmin_throws() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(RuntimeException.class);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void approve_bySuperAdmin_publishesDraftCreatedByAdmin() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.DRAFT);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.approve(1L);

        assertThat(result.getPublicationStatus()).isEqualTo(RoomPublicationStatus.PUBLISHED);
        verify(roomRepository).save(room);
    }

    // ---------- reject ----------

    @Test
    void reject_bySuperAdmin_setsRejectedAndReason() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.PENDING_APPROVAL);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.reject(1L, "Missing amenity photos");

        assertThat(result.getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Missing amenity photos");
        assertThat(result.getRejectedAt()).isNotNull();
        assertThat(result.getRejectedBy()).isEqualTo(20L);
    }

    @Test
    void reject_whenReasonBlank_throws() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.PENDING_APPROVAL);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.reject(1L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(roomRepository, never()).save(any(Room.class));
    }

    // ---------- unpublish ----------

    @Test
    void unpublish_bySuperAdmin_movesToUnpublished() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.PUBLISHED);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = service.unpublish(1L);

        assertThat(result.getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.UNPUBLISHED);
    }

    @Test
    void unpublish_whenNotPublished_throws() {
        adminRole();
        Room room = room(20L, RoomPublicationStatus.PENDING_APPROVAL);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.unpublish(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- getPublicationStatus ----------

    @Test
    void getPublicationStatus_byOwner_returnsStatus() {
        ownerRole();
        Room room = room(20L, RoomPublicationStatus.REJECTED);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThat(service.getPublicationStatus(1L))
                .isEqualTo(RoomPublicationStatus.REJECTED);
    }

    // ---------- getPendingApprovalRooms (SUPER_ADMIN) ----------

    @Test
    void getPendingApprovalRooms_bySuperAdmin_returnsPending() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.PENDING_APPROVAL)));
        when(roomRepository.findByPublicationStatus(
                eq(RoomPublicationStatus.PENDING_APPROVAL), any(Pageable.class)))
                .thenReturn(page);

        assertThat(service.getPendingApprovalRooms(anyPageable()).getContent())
                .hasSize(1)
                .first()
                .extracting(Room::getPublicationStatus)
                .isEqualTo(RoomPublicationStatus.PENDING_APPROVAL);
    }

    @Test
    void getPendingApprovalRooms_whenNotAdmin_throws() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> service.getPendingApprovalRooms(anyPageable()))
                .isInstanceOf(RuntimeException.class);
    }

    // ---------- getPublishedAvailableRooms (public) ----------

    @Test
    void getPublishedAvailableRooms_returnsPublishedAvailable() {
        Room room = room(20L, RoomPublicationStatus.PUBLISHED);
        PageImpl<Room> page = new PageImpl<>(List.of(room));
        when(roomRepository.findByPublicationStatusAndStatus(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE),
                any(Pageable.class))).thenReturn(page);

        assertThat(service.getPublishedAvailableRooms(anyPageable()).getContent())
                .hasSize(1)
                .first()
                .extracting(Room::getPublicationStatus)
                .isEqualTo(RoomPublicationStatus.PUBLISHED);
    }

    // ---------- getRoomsByPublicationStatus ----------

    @Test
    void getRoomsByPublicationStatus_asAdmin_returnsAll() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.PUBLISHED)));
        when(roomRepository.findByPublicationStatus(
                eq(RoomPublicationStatus.PUBLISHED), any(Pageable.class))).thenReturn(page);

        assertThat(service.getRoomsByPublicationStatus(RoomPublicationStatus.PUBLISHED, anyPageable()).getContent())
                .hasSize(1);
    }

    private Pageable anyPageable() {
        return org.springframework.data.domain.Pageable.unpaged();
    }

    // ---------- getRoomsForReview ----------

    @Test
    void getRoomsForReview_asAdmin_withoutFilters_returnsAllRooms() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.DRAFT)));
        when(roomRepository.findRoomsForReview(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        assertThat(service.getRoomsForReview(null, null, anyPageable()).getContent())
                .hasSize(1);
        verify(roomRepository).findRoomsForReview(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_asAdmin_withPublicationStatus_returnsFiltered() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.PENDING_APPROVAL)));
        when(roomRepository.findRoomsForReview(
                eq(RoomPublicationStatus.PENDING_APPROVAL), eq(null), any(Pageable.class)))
                .thenReturn(page);

        assertThat(service.getRoomsForReview(
                RoomPublicationStatus.PENDING_APPROVAL, null, anyPageable()).getContent())
                .hasSize(1);
        verify(roomRepository).findRoomsForReview(
                eq(RoomPublicationStatus.PENDING_APPROVAL), eq(null), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_asAdmin_publishedAndAvailable_returnsPublishedAvailable() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.PUBLISHED)));
        when(roomRepository.findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(page);

        Page<Room> result = service.getRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, anyPageable());

        assertThat(result.getContent()).hasSize(1);
        verify(roomRepository).findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_asAdmin_publishedAndOccupied_isIndependentOfPublication() {
        adminRole();
        Room occupied = room(20L, RoomPublicationStatus.PUBLISHED);
        occupied.setStatus(RoomStatus.OCCUPIED);
        PageImpl<Room> page = new PageImpl<>(List.of(occupied));
        when(roomRepository.findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.OCCUPIED), any(Pageable.class)))
                .thenReturn(page);

        Page<Room> result = service.getRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.OCCUPIED, anyPageable());

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(Room::getStatus)
                .isEqualTo(RoomStatus.OCCUPIED);
        verify(roomRepository).findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.OCCUPIED), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_asAdmin_publishedAndMaintenance_returnsFiltered() {
        adminRole();
        Room maintenance = room(20L, RoomPublicationStatus.PUBLISHED);
        maintenance.setStatus(RoomStatus.MAINTENANCE);
        PageImpl<Room> page = new PageImpl<>(List.of(maintenance));
        when(roomRepository.findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.MAINTENANCE), any(Pageable.class)))
                .thenReturn(page);

        Page<Room> result = service.getRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.MAINTENANCE, anyPageable());

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(Room::getStatus)
                .isEqualTo(RoomStatus.MAINTENANCE);
        verify(roomRepository).findRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.MAINTENANCE), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_asAdmin_roomStatusOnly_filtersAcrossAllPublications() {
        adminRole();
        PageImpl<Room> page = new PageImpl<>(List.of(room(20L, RoomPublicationStatus.DRAFT)));
        when(roomRepository.findRoomsForReview(eq(null), eq(RoomStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(page);

        assertThat(service.getRoomsForReview(null, RoomStatus.AVAILABLE, anyPageable()).getContent())
                .hasSize(1);
        verify(roomRepository).findRoomsForReview(
                eq(null), eq(RoomStatus.AVAILABLE), any(Pageable.class));
    }

    @Test
    void getRoomsForReview_whenNotAdmin_throws() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> service.getRoomsForReview(null, null, anyPageable()))
                .isInstanceOf(RuntimeException.class);
        verify(roomRepository, never()).findRoomsForReview(any(), any(), any(Pageable.class));
    }
}
