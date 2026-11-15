package com.example.spring_boot_project_api.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.response.SystemDefaultResponse;
import com.example.spring_boot_project_api.dto.response.SystemDefaultViewResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.enums.OwnerSystemDefaultStatus;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.SystemDefaultNotFoundException;
import com.example.spring_boot_project_api.mapper.SystemDefaultMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.OwnerSystemDefault;
import com.example.spring_boot_project_api.model.SystemDefault;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.OwnerSystemDefaultRepository;
import com.example.spring_boot_project_api.repository.SystemDefaultRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.SystemDefaultService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * System Default Utilities & Amenities.
 *
 * <p>SUPER_ADMIN manages reusable definitions ({@code system_defaults}). Each
 * OWNER opts in per account (see {@link #enableForOwner}). Because every
 * existing flow (rooms, meters, rates, readings, invoices) keys by an
 * owner-scoped {@code utility_types}/{@code amenities} row, enabling a default
 * materialises a real owner copy — reusing an existing owner row by
 * code/name where possible, never duplicating related data — and records the
 * link in {@code owner_system_defaults}.</p>
 *
 * <p>Authorization is enforced server-side. Admin operations require the
 * SUPER_ADMIN role explicitly (never the generic permission check, because
 * OWNER is implicitly granted every permission). Owner operations resolve the
 * owner from the authenticated user and require an OWNER (or SUPER_ADMIN)
 * actor plus the underlying module permission.</p>
 */
@Service
@RequiredArgsConstructor
public class SystemDefaultServiceImpl implements SystemDefaultService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final SystemDefaultRepository systemDefaultRepository;
    private final OwnerSystemDefaultRepository ownerSystemDefaultRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final AmenityRepository amenityRepository;
    private final SystemDefaultMapper systemDefaultMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public SystemDefaultResponse createDefault(CreateSystemDefaultRequest request) {
        requireSuperAdmin();
        validateKindFields(request.kind(), request.unit(), request.billingMethod());

        if (systemDefaultRepository.existsByKindAndCode(request.kind(), request.code())) {
            throw new DuplicateResourceException(
                    "System default code already exists for " + request.kind()
                            + ": " + request.code());
        }
        if (systemDefaultRepository.existsByKindAndName(request.kind(), request.name())) {
            throw new DuplicateResourceException(
                    "System default name already exists for " + request.kind()
                            + ": " + request.name());
        }

        SystemDefault entity = systemDefaultMapper.toEntity(request);
        return systemDefaultMapper.toResponse(systemDefaultRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemDefaultResponse> listDefaults(SystemDefaultKind kind,
            SystemDefaultStatus status, String search) {
        requireSuperAdmin();

        List<SystemDefault> defaults = systemDefaultRepository.findAll();
        String term = search == null ? null : search.trim().toLowerCase();

        return defaults.stream()
                .filter(d -> kind == null || d.getKind() == kind)
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> term == null || term.isEmpty()
                        || (d.getName() != null && d.getName().toLowerCase().contains(term))
                        || (d.getCode() != null && d.getCode().toLowerCase().contains(term)))
                .sorted(Comparator.comparing(SystemDefault::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(systemDefaultMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SystemDefaultResponse updateDefault(Long id, UpdateSystemDefaultRequest request) {
        requireSuperAdmin();

        SystemDefault entity = findDefault(id);
        SystemDefaultKind kind = entity.getKind();

        String name = request.name();
        String code = request.code();
        String unit = request.unit();

        if (kind == SystemDefaultKind.UTILITY
                && unit != null && unit.isBlank()) {
            throw new IllegalArgumentException(
                    "Unit must not be blank for utility system defaults");
        }
        if (name != null && !name.equals(entity.getName())
                && systemDefaultRepository.existsByKindAndNameAndIdNot(kind, name, id)) {
            throw new DuplicateResourceException(
                    "System default name already exists for " + kind + ": " + name);
        }
        if (code != null && !code.equals(entity.getCode())
                && systemDefaultRepository.existsByKindAndCodeAndIdNot(kind, code, id)) {
            throw new DuplicateResourceException(
                    "System default code already exists for " + kind + ": " + code);
        }

        if (name != null) {
            entity.setName(name);
        }
        if (code != null) {
            entity.setCode(code);
        }
        if (unit != null) {
            entity.setUnit(unit);
        }
        if (request.billingMethod() != null) {
            entity.setBillingMethod(request.billingMethod());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.icon() != null) {
            entity.setIcon(request.icon());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }

        return systemDefaultMapper.toResponse(systemDefaultRepository.save(entity));
    }

    @Override
    @Transactional
    public void updateDefaultStatus(Long id, SystemDefaultStatus status) {
        requireSuperAdmin();
        SystemDefault entity = findDefault(id);
        entity.setStatus(status);
        systemDefaultRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemDefaultViewResponse> listDefaultsForOwner(SystemDefaultKind kind) {
        requireOwnerActor();
        requireModuleViewPermission(kind);

        User owner = securityUtils.getCurrentUser();
        Map<Long, OwnerSystemDefault> assignmentsByDefaultId = ownerSystemDefaultRepository
                .findByOwnerId(owner.getId())
                .stream()
                .collect(Collectors.toMap(a -> a.getSystemDefault().getId(), Function.identity()));

        return systemDefaultRepository.findAll()
                .stream()
                .filter(d -> d.getStatus() == SystemDefaultStatus.ACTIVE)
                .filter(d -> kind == null || d.getKind() == kind)
                .sorted(Comparator.comparing(SystemDefault::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(d -> systemDefaultMapper.toViewResponse(d,
                        assignmentsByDefaultId.get(d.getId())))
                .toList();
    }

    @Override
    @Transactional
    public SystemDefaultViewResponse enableForOwner(Long id) {
        requireOwnerActor();

        SystemDefault entity = findDefault(id);
        if (entity.getStatus() != SystemDefaultStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "System default is not active: " + entity.getName());
        }

        User owner = securityUtils.getCurrentUser();

        if (entity.getKind() == SystemDefaultKind.UTILITY) {
            securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
            UtilityType type = resolveUtilityType(owner, entity);
            OwnerSystemDefault assignment = findOrCreateAssignment(owner, entity);
            assignment.setUtilityTypeId(type.getId());
            assignment.setStatus(OwnerSystemDefaultStatus.ENABLED);
            ownerSystemDefaultRepository.save(assignment);
            return systemDefaultMapper.toViewResponse(entity, assignment);
        }

        securityUtils.requirePermission(PermissionConstants.AMENITY_CREATE);
        Amenity amenity = resolveAmenity(owner, entity);
        OwnerSystemDefault assignment = findOrCreateAssignment(owner, entity);
        assignment.setAmenityId(amenity.getId());
        assignment.setStatus(OwnerSystemDefaultStatus.ENABLED);
        ownerSystemDefaultRepository.save(assignment);
        return systemDefaultMapper.toViewResponse(entity, assignment);
    }

    @Override
    @Transactional
    public SystemDefaultViewResponse disableForOwner(Long id) {
        requireOwnerActor();

        SystemDefault entity = findDefault(id);
        User owner = securityUtils.getCurrentUser();

        if (entity.getKind() == SystemDefaultKind.UTILITY) {
            securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        } else {
            securityUtils.requirePermission(PermissionConstants.AMENITY_UPDATE);
        }

        return ownerSystemDefaultRepository
                .findByOwnerIdAndSystemDefault(owner.getId(), entity)
                .map(assignment -> {
                    assignment.setStatus(OwnerSystemDefaultStatus.DISABLED);
                    ownerSystemDefaultRepository.save(assignment);
                    return systemDefaultMapper.toViewResponse(entity, assignment);
                })
                .orElseGet(() -> systemDefaultMapper.toViewResponse(entity, null));
    }

    /**
     * Finds or creates the owner-scoped utility type materialised for a system
     * default. Existing owner rows are reused by code first, then by name, so
     * canonical Electricity/Water rows are never duplicated.
     */
    private UtilityType resolveUtilityType(User owner, SystemDefault entity) {
        return utilityTypeRepository
                .findByOwnerIdAndCodeIgnoreCase(owner.getId(), entity.getCode())
                .or(() -> utilityTypeRepository
                        .findByOwnerIdAndNameIgnoreCase(owner.getId(), entity.getName()))
                .orElseGet(() -> utilityTypeRepository.save(UtilityType.builder()
                        .owner(owner)
                        .name(entity.getName())
                        .code(entity.getCode())
                        .unit(entity.getUnit() != null && !entity.getUnit().isBlank()
                                ? entity.getUnit()
                                : "unit")
                        .billingMethod(entity.getBillingMethod() != null
                                ? entity.getBillingMethod()
                                : UtilityBillingMethod.METERED)
                        .status(UtilityTypeStatus.ACTIVE)
                        .build()));
    }

    /**
     * Finds or creates the owner-scoped amenity materialised for a system
     * default. Existing owner rows are reused by code first, then by name.
     */
    private Amenity resolveAmenity(User owner, SystemDefault entity) {
        return amenityRepository
                .findByOwnerIdAndCodeIgnoreCase(owner.getId(), entity.getCode())
                .or(() -> amenityRepository
                        .findByOwnerIdAndNameIgnoreCase(owner.getId(), entity.getName()))
                .orElseGet(() -> amenityRepository.save(Amenity.builder()
                        .ownerId(owner.getId())
                        .name(entity.getName())
                        .code(entity.getCode())
                        .description(entity.getDescription())
                        .icon(entity.getIcon())
                        .status(AmenityStatus.ACTIVE)
                        .build()));
    }

    private OwnerSystemDefault findOrCreateAssignment(User owner, SystemDefault entity) {
        return ownerSystemDefaultRepository
                .findByOwnerIdAndSystemDefault(owner.getId(), entity)
                .orElseGet(() -> OwnerSystemDefault.builder()
                        .owner(owner)
                        .systemDefault(entity)
                        .status(OwnerSystemDefaultStatus.ENABLED)
                        .build());
    }

    private SystemDefault findDefault(Long id) {
        return systemDefaultRepository.findById(id)
                .orElseThrow(() -> SystemDefaultNotFoundException.forId(id));
    }

    private void validateKindFields(SystemDefaultKind kind, String unit,
            UtilityBillingMethod billingMethod) {
        if (kind == SystemDefaultKind.UTILITY) {
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException(
                        "Unit is required for utility system defaults");
            }
            if (billingMethod == null) {
                throw new IllegalArgumentException(
                        "Billing method is required for utility system defaults");
            }
        }
    }

    private void requireModuleViewPermission(SystemDefaultKind kind) {
        if (kind == null) {
            securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
            securityUtils.requirePermission(PermissionConstants.AMENITY_VIEW);
        } else if (kind == SystemDefaultKind.UTILITY) {
            securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        } else {
            securityUtils.requirePermission(PermissionConstants.AMENITY_VIEW);
        }
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private void requireOwnerActor() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }
}