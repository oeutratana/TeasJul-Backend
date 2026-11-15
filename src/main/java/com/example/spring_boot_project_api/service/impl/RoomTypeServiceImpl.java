package com.example.spring_boot_project_api.service.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomTypeMapper;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomTypeImage;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.RoomTypeService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RoomTypeMapper roomTypeMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(RoomTypeRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (roomTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Room type code already exists: " + request.getCode());
        }
        RoomType roomType = roomTypeMapper.toEntity(request);
        roomType.setOwnerId(currentUserId);
        return roomTypeMapper.toDto(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomTypeById(Long id) {
        return roomTypeMapper.toDto(getOwnedRoomType(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getAllRoomTypes() {
        List<RoomType> roomTypes = resolveScopedRoomTypes();
        Map<Long, String> primaryImages = resolvePrimaryImages(roomTypes);
        return roomTypes.stream()
                .map(roomType -> roomTypeMapper.toDto(roomType, primaryImages.get(roomType.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getRoomTypesByOwner(Long ownerId) {
        if (ownerId == null || !resolveScopedOwnerIds().contains(ownerId)) {
            return Collections.emptyList();
        }
        return roomTypeRepository.findByOwnerId(ownerId).stream()
                .map(roomTypeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request) {
        RoomType existing = getOwnedRoomType(id);
        if (!existing.getCode().equals(request.getCode())
                && roomTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Room type code already exists: " + request.getCode());
        }
        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setDescription(request.getDescription());
        if (request.getCapacity() != null) existing.setCapacity(request.getCapacity());
        if (request.getDefaultRent() != null) existing.setDefaultRent(request.getDefaultRent());
        if (request.getDefaultDeposit() != null) existing.setDefaultDeposit(request.getDefaultDeposit());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        return roomTypeMapper.toDto(roomTypeRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteRoomType(Long id) {
        RoomType existing = getOwnedRoomType(id);
        roomTypeRepository.delete(existing);
    }

    @Override
    public RoomType requireAccessibleRoomType(Long id) {
        return getOwnedRoomType(id);
    }

    private Map<Long, String> resolvePrimaryImages(List<RoomType> roomTypes) {
        if (roomTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new LinkedHashMap<>();
        List<RoomTypeImage> primaryImages = roomTypeImageRepository
                .findPrimaryByRoomTypeIds(roomTypes.stream().map(RoomType::getId).toList());
        for (RoomTypeImage image : primaryImages) {
            result.putIfAbsent(image.getRoomType().getId(), image.getSecureUrl());
        }
        return result;
    }

    private List<RoomType> resolveScopedRoomTypes() {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> ownerIds = userHouseAccessRepository.findOwnerIdsByUserId(securityUtils.getCurrentUserId());
            if (ownerIds.isEmpty()) {
                return Collections.emptyList();
            }
            return roomTypeRepository.findByOwnerIdIn(ownerIds);
        }
        return roomTypeRepository.findByOwnerId(securityUtils.getCurrentUserId());
    }

    private RoomType getOwnedRoomType(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return roomTypeRepository.findById(id)
                .map(roomType -> {
                    if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
                        return roomType;
                    }
                    if (!roomType.getOwnerId().equals(currentUserId)
                            && !resolveScopedOwnerIds().contains(roomType.getOwnerId())) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return roomType;
                })
                .orElseThrow(() -> RoomTypeNotFoundException.forId(id));
    }

    private List<Long> resolveScopedOwnerIds() {
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return roomTypeRepository.findAll().stream()
                    .map(RoomType::getOwnerId)
                    .distinct()
                    .toList();
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> ownerIds = userHouseAccessRepository.findOwnerIdsByUserId(securityUtils.getCurrentUserId());
            if (ownerIds.isEmpty()) {
                return Collections.emptyList();
            }
            return ownerIds;
        }
        return List.of(securityUtils.getCurrentUserId());
    }
}
