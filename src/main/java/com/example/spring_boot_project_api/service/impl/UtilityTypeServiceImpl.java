package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityTypeResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityTypeMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.UtilityTypeService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityTypeServiceImpl implements UtilityTypeService {

    private final UtilityTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final UtilityTypeMapper typeMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public UtilityTypeResponse createType(CreateUtilityTypeRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        User owner = getCurrentOwner();

        validateUniqueFields(owner.getId(), request.name(), request.code(), null);

        UtilityType type = typeMapper.toEntity(request);
        type.setOwner(owner);

        return typeMapper.toResponse(typeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UtilityTypeResponse> getTypes(String search, UtilityTypeStatus status,
            Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        List<Long> scopedOwnerIds = resolveScopedOwnerIds();
        if (scopedOwnerIds.isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        Specification<UtilityType> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("owner").get("id").in(scopedOwnerIds));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(typeRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityTypeResponse getTypeById(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        return typeMapper.toResponse(getOwnedType(id));
    }

    @Override
    @Transactional
    public UtilityTypeResponse updateType(Long id, UpdateUtilityTypeRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityType type = getOwnedType(id);
        Long ownerId = type.getOwner().getId();

        validateUniqueFields(ownerId, request.name(), request.code(), type.getId());

        if (request.name() != null) {
            type.setName(request.name());
        }
        if (request.code() != null) {
            type.setCode(request.code());
        }
        if (request.unit() != null) {
            type.setUnit(request.unit());
        }
        if (request.status() != null) {
            type.setStatus(request.status());
        }

        return typeMapper.toResponse(typeRepository.save(type));
    }

    @Override
    @Transactional
    public void deactivateType(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityType type = getOwnedType(id);
        type.setStatus(UtilityTypeStatus.INACTIVE);
        typeRepository.save(type);
    }

    private UtilityType getOwnedType(Long id) {
        List<Long> scopedOwnerIds = resolveScopedOwnerIds();
        return typeRepository.findById(id)
                .filter(t -> scopedOwnerIds.contains(t.getOwner().getId()))
                .orElseThrow(() -> UtilityTypeNotFoundException.forId(id));
    }

    private void validateUniqueFields(Long ownerId, String name, String code, Long typeId) {
        boolean isCreate = typeId == null;

        if (name != null && (isCreate
                ? typeRepository.existsByOwnerIdAndName(ownerId, name)
                : typeRepository.existsByOwnerIdAndNameAndIdNot(ownerId, name, typeId))) {
            throw new DuplicateResourceException(
                    "Utility type name already exists for this owner: " + name);
        }
        if (code != null && (isCreate
                ? typeRepository.existsByOwnerIdAndCode(ownerId, code)
                : typeRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, code, typeId))) {
            throw new DuplicateResourceException(
                    "Utility type code already exists for this owner: " + code);
        }
    }

    private List<Long> resolveScopedOwnerIds() {
        Long currentUserId = securityUtils.getCurrentUserId();
        Long currentOwnerId = getCurrentOwner().getId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return typeRepository.findAll().stream()
                    .map(type -> type.getOwner().getId())
                    .distinct()
                    .toList();
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> ownerIds = new ArrayList<>(userHouseAccessRepository.findOwnerIdsByUserId(currentUserId));
            if (!ownerIds.contains(currentOwnerId)) {
                ownerIds.add(currentOwnerId);
            }
            return ownerIds;
        }
        return List.of(currentOwnerId);
    }

    private PageResponse<UtilityTypeResponse> toPageResponse(Page<UtilityType> page) {
        List<UtilityTypeResponse> content = page.getContent().stream()
                .map(typeMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
