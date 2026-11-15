package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.BulkRoomUtilityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.dto.response.RoomUtilityResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomUtilityMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.RoomUtilityService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomUtilityServiceImpl implements RoomUtilityService {

    private final RoomUtilityRepository roomUtilityRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final UtilityTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RoomUtilityMapper mapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<RoomUtilityResponse> getRoomUtilities(Long roomId) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
        resolveOwnerId(room);

        List<RoomUtility> utilities = roomUtilityRepository.findByRoomIdAndStatus(
                roomId, UtilityTypeStatus.ACTIVE);

        List<RoomUtilityResponse> result = new ArrayList<>();
        for (RoomUtility ru : utilities) {
            UtilityType type = typeRepository.findById(ru.getUtilityTypeId()).orElse(null);
            if (type == null) continue;
            result.add(mapper.toResponse(ru, type, room.getRoomNumber()));
        }
        return result;
    }

    @Override
    @Transactional
    public RoomUtilityResponse assignUtility(Long roomId, Long utilityTypeId, Boolean isIncluded) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_MANAGE);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
        Long ownerId = resolveOwnerId(room);

        UtilityType type = typeRepository.findByIdAndOwnerId(utilityTypeId, ownerId)
                .orElseThrow(() -> UtilityTypeNotFoundException.forId(utilityTypeId));

        Optional<RoomUtility> existing = roomUtilityRepository.findByRoomIdAndUtilityTypeId(
                roomId, utilityTypeId);

        if (existing.isPresent()) {
            RoomUtility ru = existing.get();
            if (ru.getStatus() == UtilityTypeStatus.ACTIVE) {
                throw new DuplicateResourceException(
                        "Utility type " + type.getName() + " is already assigned to this room");
            }
            ru.setStatus(UtilityTypeStatus.ACTIVE);
            if (isIncluded != null) {
                ru.setIsIncluded(isIncluded);
            }
            RoomUtility saved = roomUtilityRepository.save(ru);
            return mapper.toResponse(saved, type, room.getRoomNumber());
        }

        RoomUtility ru = RoomUtility.builder()
                .owner(userRepository.findById(ownerId).orElseThrow())
                .roomId(roomId)
                .utilityTypeId(utilityTypeId)
                .status(UtilityTypeStatus.ACTIVE)
                .isIncluded(Boolean.TRUE.equals(isIncluded))
                .build();

        RoomUtility saved = roomUtilityRepository.save(ru);
        return mapper.toResponse(saved, type, room.getRoomNumber());
    }

    @Override
    @Transactional
    public BulkAssignmentResponse bulkAssignUtilities(BulkRoomUtilityRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_MANAGE);
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        List<Long> roomIds = distinctIds(request.roomIds());
        List<Long> utilityTypeIds = distinctIds(request.utilityTypeIds());
        if (roomIds.isEmpty() || utilityTypeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one room and one utility type are required");
        }

        List<BulkAssignmentResponse.Failure> failures = new ArrayList<>();
        Map<Long, Room> roomsById = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Function.identity()));
        for (Long roomId : roomIds) {
            if (!roomsById.containsKey(roomId)) {
                for (Long typeId : utilityTypeIds) {
                    failures.add(BulkAssignmentResponse.Failure.of(roomId, typeId, "Room not found"));
                }
            }
        }

        Map<Long, Map<Long, RoomUtility>> byRoomByType = new HashMap<>();
        for (RoomUtility ru : roomUtilityRepository.findByRoomIdIn(roomIds)) {
            byRoomByType.computeIfAbsent(ru.getRoomId(), k -> new HashMap<>())
                    .put(ru.getUtilityTypeId(), ru);
        }

        Map<Long, Long> ownerByRoom = new HashMap<>();
        for (Room room : roomsById.values()) {
            Long ownerId;
            try {
                ownerId = resolveOwnerId(room);
            } catch (HouseNotFoundException e) {
                for (Long typeId : utilityTypeIds) {
                    failures.add(BulkAssignmentResponse.Failure.of(room.getId(), typeId, "House not found"));
                }
                continue;
            }
            ownerByRoom.put(room.getId(), ownerId);
        }

        Map<Long, User> ownerCache = new HashMap<>();
        int created = 0;
        int alreadyAssigned = 0;
        for (Long roomId : roomIds) {
            Room room = roomsById.get(roomId);
            if (room == null) {
                continue;
            }
            Long ownerId = ownerByRoom.get(roomId);
            if (ownerId == null) {
                continue;
            }
            Map<Long, RoomUtility> byType = byRoomByType.getOrDefault(roomId, Map.of());
            for (Long typeId : utilityTypeIds) {
                UtilityType type = typeRepository.findByIdAndOwnerId(typeId, ownerId).orElse(null);
                if (type == null) {
                    failures.add(BulkAssignmentResponse.Failure.of(roomId, typeId,
                            "Utility type does not belong to the house owner"));
                    continue;
                }
                RoomUtility existing = byType.get(typeId);
                if (existing != null && existing.getStatus() == UtilityTypeStatus.ACTIVE) {
                    alreadyAssigned++;
                    continue;
                }
                if (existing != null) {
                    existing.setStatus(UtilityTypeStatus.ACTIVE);
                    if (request.isIncluded() != null) {
                        existing.setIsIncluded(request.isIncluded());
                    }
                    roomUtilityRepository.save(existing);
                    created++;
                    continue;
                }
                User owner = ownerCache.computeIfAbsent(ownerId,
                        id -> userRepository.findById(id).orElse(null));
                if (owner == null) {
                    failures.add(BulkAssignmentResponse.Failure.of(roomId, typeId,
                            "Owner account not found"));
                    continue;
                }
                roomUtilityRepository.save(RoomUtility.builder()
                        .owner(owner)
                        .roomId(roomId)
                        .utilityTypeId(typeId)
                        .status(UtilityTypeStatus.ACTIVE)
                        .isIncluded(Boolean.TRUE.equals(request.isIncluded()))
                        .build());
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

    @Override
    @Transactional
    public void removeUtility(Long roomId, Long utilityTypeId) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_MANAGE);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
        resolveOwnerId(room);

        List<RoomUtility> list = roomUtilityRepository.findByRoomIdAndStatus(
                roomId, UtilityTypeStatus.ACTIVE);
        RoomUtility existing = list.stream()
                .filter(ru -> ru.getUtilityTypeId().equals(utilityTypeId))
                .findFirst()
                .orElseThrow(() -> new InvalidUtilityOperationException(
                        "Utility type is not assigned to this room"));

        existing.setStatus(UtilityTypeStatus.INACTIVE);
        roomUtilityRepository.save(existing);
    }

    private Long resolveOwnerId(Room room) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> HouseNotFoundException.forId(room.getHouseId()));

        Long ownerId = house.getOwner().getId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return ownerId;
        }

        if (ownerId.equals(currentUserId)) {
            return currentUserId;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            if (!userHouseAccessRepository.existsByUserIdAndHouseId(
                    currentUserId, house.getId())) {
                throw new AccessDeniedException("You do not have access to this house");
            }
            return ownerId;
        }

        throw new AccessDeniedException("You do not have permission to manage room utilities");
    }
}
