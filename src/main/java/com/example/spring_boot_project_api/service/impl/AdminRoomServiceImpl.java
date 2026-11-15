package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AdminBulkCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.AdminCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.RoomTypeMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.AdminRoomService;
import com.example.spring_boot_project_api.util.PropertyTypeRules;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoomServiceImpl implements AdminRoomService {

    private static final String ACCESS_DENIED_MSG = "You do not have permission to access this resource";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final HouseMapper houseMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getOwnerHouses(Long ownerId) {
        requireSuperAdmin();
        findOwner(ownerId);
        return houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(houseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getOwnerRoomTypes(Long ownerId) {
        requireSuperAdmin();
        findOwner(ownerId);
        return roomTypeRepository.findByOwnerId(ownerId).stream()
                .map(roomTypeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse createRoomForOwner(Long ownerId, AdminCreateRoomRequest request) {
        requireSuperAdmin();
        findOwner(ownerId);
        return roomMapper.toDto(saveRoom(ownerId, request, Boolean.TRUE.equals(request.getPublishImmediately())));
    }

    @Override
    @Transactional
    public List<RoomResponse> createRoomsForOwner(Long ownerId, AdminBulkCreateRoomRequest request) {
        requireSuperAdmin();
        findOwner(ownerId);
        boolean publishImmediately = Boolean.TRUE.equals(request.getPublishImmediately());
        return request.getRooms().stream()
                .map(room -> roomMapper.toDto(saveRoom(ownerId, room, publishImmediately)))
                .toList();
    }

    @Override
    @Transactional
    public HouseResponse createHouseForOwner(Long ownerId, CreateHouseRequest request) {
        requireSuperAdmin();
        User owner = findOwner(ownerId);
        validateHouseUniqueForOwner(ownerId, request);

        House house = houseMapper.toEntity(request);
        house.setOwner(owner);
        return houseMapper.toResponse(houseRepository.save(house));
    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomTypeForOwner(Long ownerId, RoomTypeRequest request) {
        requireSuperAdmin();
        findOwner(ownerId);
        if (roomTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Room type code already exists: " + request.getCode());
        }

        RoomType roomType = roomTypeMapper.toEntity(request);
        roomType.setOwnerId(ownerId);
        return roomTypeMapper.toDto(roomTypeRepository.save(roomType));
    }

    /**
     * Creates a single physical room for the owner after verifying that the
     * house and (when provided) room type belong to that owner. When
     * {@code publishImmediately} is true the room is created as PUBLISHED and
     * approved by the current admin.
     */
    private Room saveRoom(Long ownerId, AdminCreateRoomRequest request, boolean publishImmediately) {
        houseRepository.findByIdAndOwnerId(request.getHouseId(), ownerId)
                .orElseThrow(() -> HouseNotFoundException.forId(request.getHouseId()));

        // Validate and normalise room-identity fields per property type.
        PropertyTypeRules.validate(
                request.getPropertyType(),
                request.getRoomTypeId(),
                request.getRoomNumber(),
                request.getFloorNumber(),
                request.getCapacity());

        if (PropertyTypeRules.isStructureLike(request.getPropertyType())) {
            // Structural listings (HOUSE / VILLA / OTHER) are whole-property
            // rows — room-identity fields are not applicable.
            request.setRoomTypeId(null);
            request.setRoomNumber(null);
            request.setFloorNumber(null);
        }

        if (request.getRoomTypeId() != null) {
            roomTypeRepository.findById(request.getRoomTypeId())
                    .filter(roomType -> ownerId.equals(roomType.getOwnerId()))
                    .orElseThrow(() -> RoomTypeNotFoundException.forId(request.getRoomTypeId()));
        }

        if (request.getRoomNumber() != null && !request.getRoomNumber().isBlank()
                && roomRepository.existsByHouseIdAndRoomNumber(
                        request.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }

        Room room = roomMapper.toEntity(request);
        room.setOwnerId(ownerId);

        if (publishImmediately) {
            LocalDateTime now = LocalDateTime.now();
            room.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
            room.setApprovedAt(now);
            room.setApprovedBy(securityUtils.getCurrentUserId());
            room.setPublishedAt(now);
        }

        return roomRepository.save(room);
    }

    private void validateHouseUniqueForOwner(Long ownerId, CreateHouseRequest request) {
        if (houseRepository.existsByOwnerIdAndName(ownerId, request.name())) {
            throw new DuplicateResourceException("House name already exists for this owner: " + request.name());
        }
        if (houseRepository.existsByOwnerIdAndSlug(ownerId, request.slug())) {
            throw new DuplicateResourceException("House slug already exists for this owner: " + request.slug());
        }
        if (houseRepository.existsByOwnerIdAndCode(ownerId, request.code())) {
            throw new DuplicateResourceException("House code already exists for this owner: " + request.code());
        }
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private User findOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> UserNotFoundException.forId(ownerId));
        if (!userRoleRepository.existsByUserIdAndRoleCode(ownerId, SecurityUtils.ROLE_OWNER)) {
            throw UserNotFoundException.forId(ownerId);
        }
        return owner;
    }
}