package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.BulkRoomAmenityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.dto.response.RoomAmenityResponse;
import com.example.spring_boot_project_api.exception.AmenityNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoomAmenityNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomAmenityMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomAmenity;
import com.example.spring_boot_project_api.model.RoomAmenityId;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.RoomAmenityService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomAmenityServiceImpl implements RoomAmenityService {

    private final RoomAmenityRepository roomAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomAmenityMapper roomAmenityMapper;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoomAmenityResponse assignAmenityToRoom(Long roomId, Long amenityId) {
        Long currentUserId = getCurrentUser().getId();
        houseAccessService.resolveRoomHouseId(roomId);

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> AmenityNotFoundException.forId(amenityId));

        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
            && !amenity.getOwnerId().equals(currentUserId)
                && !houseAccessService.getScopedOwnerIds().contains(amenity.getOwnerId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this resource");
        }

        if (roomAmenityRepository.existsByIdRoomIdAndIdAmenityId(roomId, amenityId)) {
            throw new DuplicateResourceException(
                    "Amenity " + amenityId + " is already assigned to room " + roomId);
        }

        return roomAmenityMapper.toResponse(
                roomAmenityRepository.save(roomAmenityMapper.toEntity(roomId, amenity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAmenityResponse> getRoomAmenities(Long roomId) {
        houseAccessService.resolveRoomHouseId(roomId);
        return roomAmenityRepository.findByIdRoomId(roomId).stream()
                .map(roomAmenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeAmenityFromRoom(Long roomId, Long amenityId) {
        houseAccessService.resolveRoomHouseId(roomId);
        if (!roomAmenityRepository.existsByIdRoomIdAndIdAmenityId(roomId, amenityId)) {
            throw RoomAmenityNotFoundException.forIds(roomId, amenityId);
        }
        roomAmenityRepository.deleteByIdRoomIdAndIdAmenityId(roomId, amenityId);
    }

    @Override
    @Transactional
    public BulkAssignmentResponse bulkAssignAmenities(BulkRoomAmenityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        List<Long> roomIds = distinctIds(request.roomIds());
        List<Long> amenityIds = distinctIds(request.amenityIds());
        if (roomIds.isEmpty() || amenityIds.isEmpty()) {
            throw new IllegalArgumentException("At least one room and one amenity are required");
        }

        List<BulkAssignmentResponse.Failure> failures = new ArrayList<>();
        Map<Long, Room> roomsById = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Function.identity()));
        for (Long roomId : roomIds) {
            if (!roomsById.containsKey(roomId)) {
                for (Long amenityId : amenityIds) {
                    failures.add(BulkAssignmentResponse.Failure.of(roomId, amenityId, "Room not found"));
                }
            }
        }

        for (Room room : roomsById.values()) {
            houseAccessService.requireHouseAccess(room.getHouseId());
        }

        Long currentUserId = getCurrentUser().getId();
        List<Long> scopedOwnerIds = houseAccessService.getScopedOwnerIds();
        Map<Long, Amenity> amenitiesById = new HashMap<>();
        for (Long amenityId : amenityIds) {
            Amenity amenity = amenityRepository.findById(amenityId).orElse(null);
            if (amenity == null) {
                for (Long roomId : roomIds) {
                    if (roomsById.containsKey(roomId)) {
                        failures.add(BulkAssignmentResponse.Failure.of(roomId, amenityId, "Amenity not found"));
                    }
                }
                continue;
            }
            if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
                && !amenity.getOwnerId().equals(currentUserId)
                && !scopedOwnerIds.contains(amenity.getOwnerId())) {
                throw new AccessDeniedException(
                        "You do not have permission to access this resource");
            }
            amenitiesById.put(amenityId, amenity);
        }

        Set<RoomAmenityId> existingAssignments = roomAmenityRepository.findByIdRoomIdIn(roomIds).stream()
                .map(RoomAmenity::getId)
                .collect(Collectors.toSet());

        int created = 0;
        int alreadyAssigned = 0;
        for (Long roomId : roomIds) {
            if (!roomsById.containsKey(roomId)) {
                continue;
            }
            for (Long amenityId : amenityIds) {
                Amenity amenity = amenitiesById.get(amenityId);
                if (amenity == null) {
                    continue;
                }
                if (existingAssignments.contains(new RoomAmenityId(roomId, amenityId))) {
                    alreadyAssigned++;
                    continue;
                }
                roomAmenityRepository.save(roomAmenityMapper.toEntity(roomId, amenity));
                created++;
            }
        }
        int roomsProcessed = (int) roomIds.stream().filter(roomsById::containsKey).count();
        return BulkAssignmentResponse.of(roomsProcessed, created, alreadyAssigned, failures);
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
