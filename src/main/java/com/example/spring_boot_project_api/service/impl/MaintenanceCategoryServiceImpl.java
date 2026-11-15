package com.example.spring_boot_project_api.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCategoryResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.MaintenanceCategoryNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.MaintenanceCategoryMapper;
import com.example.spring_boot_project_api.model.MaintenanceCategory;
import com.example.spring_boot_project_api.model.MaintenanceCategoryStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.MaintenanceCategoryRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.MaintenanceCategoryService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintenanceCategoryServiceImpl implements MaintenanceCategoryService {

    private final MaintenanceCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final MaintenanceCategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public MaintenanceCategoryResponse createCategory(CreateMaintenanceCategoryRequest request) {
        User owner = getCurrentOwner();

        validateUniqueFields(owner.getId(), request.name(), request.code(), null);

        MaintenanceCategory category = categoryMapper.toEntity(request);
        category.setOwner(owner);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceCategoryResponse> getMyCategories() {
        List<Long> ownerIds = resolveScopedOwnerIds();
        if (ownerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return categoryRepository.findAllByOwnerIdInOrderByCreatedAtDesc(ownerIds).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceCategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(getOwnedCategory(id));
    }

    @Override
    @Transactional
    public MaintenanceCategoryResponse updateCategory(Long id, UpdateMaintenanceCategoryRequest request) {
        MaintenanceCategory category = getOwnedCategory(id);
        Long ownerId = category.getOwner().getId();

        validateUniqueFields(ownerId, request.name(), request.code(), category.getId());

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.code() != null) {
            category.setCode(request.code());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.status() != null) {
            category.setStatus(request.status());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deactivateCategory(Long id) {
        MaintenanceCategory category = getOwnedCategory(id);
        category.setStatus(MaintenanceCategoryStatus.INACTIVE);
        categoryRepository.save(category);
    }

    private MaintenanceCategory getOwnedCategory(Long id) {
        Long currentUserId = getCurrentOwner().getId();

        MaintenanceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> MaintenanceCategoryNotFoundException.forId(id));
        Long categoryOwnerId = category.getOwner().getId();
        if (categoryOwnerId.equals(currentUserId)) {
            return category;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && userHouseAccessRepository.findOwnerIdsByUserId(currentUserId).contains(categoryOwnerId)) {
            return category;
        }
        throw MaintenanceCategoryNotFoundException.forId(id);
    }

    private void validateUniqueFields(Long ownerId, String name, String code, Long categoryId) {
        boolean isCreate = categoryId == null;

        if (name != null && (isCreate
                ? categoryRepository.existsByOwnerIdAndName(ownerId, name)
                : categoryRepository.existsByOwnerIdAndNameAndIdNot(ownerId, name, categoryId))) {
            throw new DuplicateResourceException(
                    "Maintenance category name already exists for this owner: " + name);
        }
        if (code != null && (isCreate
                ? categoryRepository.existsByOwnerIdAndCode(ownerId, code)
                : categoryRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, code, categoryId))) {
            throw new DuplicateResourceException(
                    "Maintenance category code already exists for this owner: " + code);
        }
    }

    private List<Long> resolveScopedOwnerIds() {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            return userHouseAccessRepository.findOwnerIdsByUserId(securityUtils.getCurrentUserId());
        }
        return List.of(getCurrentOwner().getId());
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
