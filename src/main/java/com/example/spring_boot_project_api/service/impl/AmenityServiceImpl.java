package com.example.spring_boot_project_api.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AmenityRequest;
import com.example.spring_boot_project_api.dto.request.UpdateAmenityRequest;
import com.example.spring_boot_project_api.dto.response.AmenityResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.exception.AmenityNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.AmenityMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AmenityService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final AmenityMapper amenityMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public AmenityResponse createAmenity(AmenityRequest request) {
        securityUtils.requirePermission(PermissionConstants.AMENITY_CREATE);
        Long ownerId = getCurrentUser().getId();

        validateUniqueFields(ownerId, request.name(), request.code(), null);

        Amenity amenity = amenityMapper.toEntity(request);
        amenity.setOwnerId(ownerId);

        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional(readOnly = true)
    public AmenityResponse getAmenityById(Long id) {
        securityUtils.requirePermission(PermissionConstants.AMENITY_VIEW);
        return amenityMapper.toResponse(getOwnedAmenity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getMyAmenities() {
        securityUtils.requirePermission(PermissionConstants.AMENITY_VIEW);
        List<Long> ownerIds = resolveScopedOwnerIds();
        return amenityRepository.findByOwnerIdInOrderByCreatedAtDesc(ownerIds).stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getMyAmenitiesByStatus(AmenityStatus status) {
        securityUtils.requirePermission(PermissionConstants.AMENITY_VIEW);
        List<Long> ownerIds = resolveScopedOwnerIds();
        return amenityRepository.findByOwnerIdInAndStatus(ownerIds, status).stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AmenityResponse updateAmenity(Long id, UpdateAmenityRequest request) {
        securityUtils.requirePermission(PermissionConstants.AMENITY_UPDATE);
        Amenity amenity = getOwnedAmenity(id);
        Long ownerId = amenity.getOwnerId();

        validateUniqueFields(ownerId, request.name(), request.code(), amenity.getId());

        if (request.name() != null) {
            amenity.setName(request.name());
        }
        if (request.code() != null) {
            amenity.setCode(request.code());
        }
        if (request.description() != null) {
            amenity.setDescription(request.description());
        }
        if (request.icon() != null) {
            amenity.setIcon(request.icon());
        }
        if (request.status() != null) {
            amenity.setStatus(request.status());
        }

        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional
    public void deleteAmenity(Long id) {
        securityUtils.requirePermission(PermissionConstants.AMENITY_DELETE);
        Amenity amenity = getOwnedAmenity(id);
        amenityRepository.delete(amenity);
    }

    private List<Long> resolveScopedOwnerIds() {
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return amenityRepository.findAll().stream()
                    .map(Amenity::getOwnerId)
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
        return List.of(getCurrentUser().getId());
    }

    private Amenity getOwnedAmenity(Long id) {
        Long currentUserId = getCurrentUser().getId();

        return amenityRepository.findById(id)
                .map(amenity -> {
                    if (!amenity.getOwnerId().equals(currentUserId)
                            && !resolveScopedOwnerIds().contains(amenity.getOwnerId())) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return amenity;
                })
                .orElseThrow(() -> AmenityNotFoundException.forId(id));
    }

    private void validateUniqueFields(Long ownerId, String name, String code, Long amenityId) {
        boolean isCreate = amenityId == null;

        if (name != null && (isCreate
                ? amenityRepository.existsByOwnerIdAndName(ownerId, name)
                : amenityRepository.existsByOwnerIdAndNameAndIdNot(ownerId, name, amenityId))) {
            throw new DuplicateResourceException("Amenity name already exists for this owner: " + name);
        }
        if (code != null && (isCreate
                ? amenityRepository.existsByOwnerIdAndCode(ownerId, code)
                : amenityRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, code, amenityId))) {
            throw new DuplicateResourceException("Amenity code already exists for this owner: " + code);
        }
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
