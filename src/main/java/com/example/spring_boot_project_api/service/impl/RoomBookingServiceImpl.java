package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomBookingRequest;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;
import com.example.spring_boot_project_api.enums.RoomBookingStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.RoomBookingNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidRoomBookingOperationException;
import com.example.spring_boot_project_api.mapper.RoomBookingMapper;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomBooking;
import com.example.spring_boot_project_api.repository.RoomBookingRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.RoomBookingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomBookingServiceImpl implements RoomBookingService {
    private final RoomBookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RoomBookingMapper bookingMapper;
    private final SecurityUtils securityUtils;

    @Override
    public RoomBookingResponse createBooking(RoomBookingRequest request) {
        securityUtils.requirePermission(PermissionConstants.ROOM_CREATE);
        Room room = getAccessibleRoomForBooking(request.getRoomId());
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new InvalidRoomBookingOperationException("Only an available room can be booked");
        }
        RoomBooking booking = new RoomBooking();
        booking.setRoomId(room.getId());
        booking.setCustomerName(request.getCustomerName().trim());
        booking.setCustomerPhone(request.getCustomerPhone().trim());
        booking.setCustomerEmail(blankToNull(request.getCustomerEmail()));
        booking.setBookingDate(LocalDate.now());
        booking.setExpectedMoveInDate(request.getExpectedMoveInDate());
        booking.setStatus(RoomBookingStatus.BOOKED);
        booking.setNote(blankToNull(request.getNote()));
        RoomBooking saved = bookingRepository.save(booking);
        room.setStatus(RoomStatus.BOOKED);
        roomRepository.save(room);
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomBookingResponse getBookingById(Long id) {
        securityUtils.requirePermission(PermissionConstants.ROOM_VIEW);
        RoomBooking booking = findBooking(id);
        getAccessibleRoom(booking.getRoomId());
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomBookingResponse> getBookings(Long roomId) {
        securityUtils.requirePermission(PermissionConstants.ROOM_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Room> rooms;
        if (roomId != null) {
            rooms = List.of(getAccessibleRoom(roomId));
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            rooms = roomRepository.findAll();
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> houseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            rooms = houseIds.isEmpty() ? Collections.emptyList() : roomRepository.findByHouseIdIn(houseIds);
        } else {
            rooms = roomRepository.findByOwnerId(currentUserId);
        }
        if (rooms.isEmpty()) return List.of();
        return bookingRepository.findByRoomIdInOrderByCreatedAtDesc(rooms.stream().map(Room::getId).toList())
                .stream().map(bookingMapper::toResponse).toList();
    }

    @Override
    public RoomBookingResponse cancelBooking(Long id) {
        securityUtils.requirePermission(PermissionConstants.ROOM_UPDATE);
        RoomBooking booking = findBooking(id);
        Room room = getAccessibleRoom(booking.getRoomId());
        if (booking.getStatus() != RoomBookingStatus.BOOKED) {
            throw new InvalidRoomBookingOperationException("Only a booked room reservation can be cancelled");
        }
        booking.setStatus(RoomBookingStatus.CANCELLED);
        RoomBooking saved = bookingRepository.save(booking);
        if (room.getStatus() == RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
        return bookingMapper.toResponse(saved);
    }

    @Override
    public Optional<RoomBooking> findCurrentBookingForRoom(Long roomId) {
        return bookingRepository.findFirstByRoomIdAndStatusOrderByCreatedAtDesc(roomId, RoomBookingStatus.BOOKED);
    }

    @Override
    public void completeBookingForContract(Long bookingId, Long roomId) {
        RoomBooking booking = bookingRepository.findById(bookingId)
                .filter(candidate -> candidate.getRoomId().equals(roomId)
                        && candidate.getStatus() == RoomBookingStatus.BOOKED)
                .orElseThrow(() -> new InvalidRoomBookingOperationException(
                        "The selected room no longer has the booking required for this contract"));
        booking.setStatus(RoomBookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    private RoomBooking findBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> RoomBookingNotFoundException.forId(id));
    }

    private Room getAccessibleRoom(Long roomId) {
        return roomRepository.findById(roomId).map(this::ensureRoomAccess)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    /** Locks the room while checking availability so concurrent requests cannot both book it. */
    private Room getAccessibleRoomForBooking(Long roomId) {
        return roomRepository.findWithLockById(roomId).map(this::ensureRoomAccess)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    private Room ensureRoomAccess(Room room) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (room.getOwnerId().equals(currentUserId)
                || securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
                || (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                        && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, room.getHouseId()))) {
            return room;
        }
        throw new AccessDeniedException("You do not have permission to access this resource");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
