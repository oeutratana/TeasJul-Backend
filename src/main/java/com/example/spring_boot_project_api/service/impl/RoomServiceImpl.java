package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.RoomService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.PropertyTypeRules;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final RentalContractRepository rentalContractRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RoomMapper roomMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRepuest request) {
        securityUtils.requirePermission(PermissionConstants.ROOM_CREATE);
        Long currentUserId = securityUtils.getCurrentUserId();
        Long ownerId = resolveHouseOwnerId(request.getHouseId(), currentUserId);

        if (request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (roomRepository.existsByHouseIdAndRoomNumber(request.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }
        Room room = roomMapper.toEntity(request);
        room.setOwnerId(ownerId);
        return roomMapper.toDto(roomRepository.save(room), null);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        securityUtils.requirePermission(PermissionConstants.ROOM_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();
        Room room = getAccessibleRoom(id);
        RoomTenantResponse tenant = getActiveTenantForRoom(room);
        return roomMapper.toDto(room, tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        securityUtils.requirePermission(PermissionConstants.ROOM_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Room> rooms;

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> assignedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (assignedHouseIds.isEmpty()) {
                return Collections.emptyList();
            }
            rooms = roomRepository.findByHouseIdIn(assignedHouseIds);
        } else {
            rooms = roomRepository.findByOwnerId(currentUserId);
        }

        Map<Long, RoomTenantResponse> tenantMap = getActiveTenantsForRooms(rooms);
        return rooms.stream()
                .map(room -> roomMapper.toDto(room, tenantMap.get(room.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHouse(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.ROOM_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyHouseAccess(houseId, currentUserId);
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        Map<Long, RoomTenantResponse> tenantMap = getActiveTenantsForRooms(rooms);
        return rooms.stream()
                .map(room -> roomMapper.toDto(room, tenantMap.get(room.getId())))
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRepuest request) {
        securityUtils.requirePermission(PermissionConstants.ROOM_UPDATE);
        Room existing = getAccessibleRoom(id);
        Long currentUserId = securityUtils.getCurrentUserId();

        Long targetHouseId = request.getHouseId();
        if (targetHouseId != null && !targetHouseId.equals(existing.getHouseId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this resource");
        }
        verifyHouseAccess(existing.getHouseId(), currentUserId);

        // Enforce per-property-type validation for the SUPER_ADMIN direct
        // posting workflow (including subsequent draft edits). OWNER/MANAGER
        // workflows keep their existing behaviour.
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
                && request.getPropertyType() != null) {
            PropertyTypeRules.validate(
                    request.getPropertyType(),
                    request.getRoomTypeId(),
                    request.getRoomNumber(),
                    request.getFloorNumber(),
                    request.getCapacity());
        }

        String roomNumber = request.getRoomNumber();
        if (roomNumber != null && !Objects.equals(existing.getRoomNumber(), roomNumber)
                && roomRepository.existsByHouseIdAndRoomNumber(existing.getHouseId(), roomNumber)) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + roomNumber);
        }

        existing.setRoomTypeId(request.getRoomTypeId());
        if (roomNumber != null) {
            existing.setRoomNumber(roomNumber);
        }
        existing.setName(request.getName());
        existing.setFloorNumber(request.getFloorNumber());
        if (request.getMonthlyRent() != null) {
            existing.setMonthlyRent(request.getMonthlyRent());
        }
        if (request.getSecurityDeposit() != null) {
            existing.setSecurityDeposit(request.getSecurityDeposit());
        }
        if (request.getCapacity() != null) {
            existing.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setDescription(request.getDescription());

        if (request.getPropertyType() != null) {
            existing.setPropertyType(request.getPropertyType());
        }
        if (request.getListingScope() != null) {
            existing.setListingScope(request.getListingScope());
        }
        if (request.getFurnishing() != null) {
            existing.setFurnishing(request.getFurnishing());
        }
        if (request.getAvailableFrom() != null) {
            existing.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAdvancePayment() != null) {
            existing.setAdvancePayment(request.getAdvancePayment());
        }
        if (request.getBedrooms() != null) {
            existing.setBedrooms(request.getBedrooms());
        }
        if (request.getBathrooms() != null) {
            existing.setBathrooms(request.getBathrooms());
        }
        if (request.getAreaSqm() != null) {
            existing.setAreaSqm(request.getAreaSqm());
        }
        if (request.getLandSizeSqm() != null) {
            existing.setLandSizeSqm(request.getLandSizeSqm());
        }
        if (request.getNumberOfFloors() != null) {
            existing.setNumberOfFloors(request.getNumberOfFloors());
        }

        Room saved = roomRepository.save(existing);
        RoomTenantResponse tenant = getActiveTenantForRoom(saved);
        return roomMapper.toDto(saved, tenant);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        securityUtils.requirePermission(PermissionConstants.ROOM_DELETE);
        Room existing = getAccessibleRoom(id);
        roomRepository.delete(existing);
    }

    private Map<Long, RoomTenantResponse> getActiveTenantsForRooms(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        List<Long> ownerIds = rooms.stream().map(Room::getOwnerId).distinct().toList();
        List<ActiveRoomTenantDto> activeTenants = rentalContractRepository.findActiveTenantsByRoomIds(
                roomIds, ownerIds, LocalDate.now()
        );
        Map<Long, RoomTenantResponse> map = new HashMap<>();
        for (ActiveRoomTenantDto item : activeTenants) {
            map.putIfAbsent(item.roomId(), new RoomTenantResponse(item.tenantId(), item.firstName(), item.lastName()));
        }
        return map;
    }

    private RoomTenantResponse getActiveTenantForRoom(Room room) {
        List<ActiveRoomTenantDto> list = rentalContractRepository.findActiveTenantsByRoomIds(
                List.of(room.getId()), List.of(room.getOwnerId()), LocalDate.now()
        );
        if (list.isEmpty()) {
            return null;
        }
        ActiveRoomTenantDto item = list.get(0);
        return new RoomTenantResponse(item.tenantId(), item.firstName(), item.lastName());
    }

    private Room getAccessibleRoom(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return roomRepository.findById(id)
                .map(room -> {
                    if (canAccess(room, currentUserId)) {
                        return room;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RoomNotFoundException.forId(id));
    }

    private boolean canAccess(Room room, Long currentUserId) {
        return room.getOwnerId().equals(currentUserId)
                || securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)
                || (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                        && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, room.getHouseId()));
    }

    private void verifyHouseOwnership(Long houseId, Long currentUserId) {
        houseRepository.findByIdAndOwnerId(houseId, currentUserId)
                .map(House::getId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }

    private void verifyHouseAccess(Long houseId, Long currentUserId) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            if (!userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
                throw new AccessDeniedException(
                        "You do not have permission to access this house");
            }
        } else {
            verifyHouseOwnership(houseId, currentUserId);
        }
    }

    private Long resolveHouseOwnerId(Long houseId, Long currentUserId) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            House house = houseRepository.findById(houseId)
                    .orElseThrow(() -> HouseNotFoundException.forId(houseId));
            if (!userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
                throw new AccessDeniedException(
                        "You do not have permission to access this house");
            }
            return house.getOwner().getId();
        }
        return houseRepository.findByIdAndOwnerId(houseId, currentUserId)
                .map(House::getOwner)
                .map(User::getId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }
}
