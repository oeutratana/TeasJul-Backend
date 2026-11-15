package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.dto.response.TenantSummaryResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.TenantService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final TenantMapper tenantMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        securityUtils.requirePermission(PermissionConstants.TENANT_CREATE);
        User owner = getCurrentOwner();

        // The user link is optional: a tenant profile can exist without a login account.
        User tenantUser = null;
        if (request.userId() != null) {
            tenantUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> UserNotFoundException.forId(request.userId()));

            // The linked account must fall inside the caller's authorized scope, so
            // an OWNER/MANAGER can never attach a user they do not manage to a
            // tenant profile. Owner attribution below is always server-derived.
            requireUserWithinCallerScope(tenantUser.getId());

            // A user account can be linked to at most one tenant profile.
            if (tenantRepository.findByUserId(tenantUser.getId()).isPresent()) {
                throw new DuplicateResourceException("User already has a tenant profile");
            }
        }

        validateUniqueFields(owner.getId(), request.tenantCode(), request.phone(), null);

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setUser(tenantUser);
        tenant.setOwner(owner);
        tenant.setInvitedBy(owner);

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    private static final int MAX_LIST_SIZE = 200;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> getMyTenants(String search, TenantStatus status, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.TENANT_VIEW);
        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE),
                pageable.getSort());
        String q = normalize(search);

        List<Long> houseIds = null;
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER) && !securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            houseIds = houseAccessService.getAccessibleHouseIds();
            if (houseIds.isEmpty()) {
                return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
            }
        }

        Long ownerId = (houseIds == null) ? securityUtils.getCurrentUserId() : null;
        org.springframework.data.domain.Page<Tenant> page =
                tenantRepository.findAll(tenantFilters(ownerId, houseIds, q, status), capped);

        List<TenantResponse> content = page.getContent().stream()
                .map(tenantMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSummaryResponse getMyTenantsSummary() {
        securityUtils.requirePermission(PermissionConstants.TENANT_VIEW);
        List<Object[]> rows;
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER) && !securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            List<Long> houseIds = houseAccessService.getAccessibleHouseIds();
            if (houseIds.isEmpty()) {
                return new TenantSummaryResponse(0L, java.util.Map.of());
            }
            rows = tenantRepository.countGroupByHouseIds(houseIds);
        } else {
            rows = tenantRepository.countGroupByOwnerId(securityUtils.getCurrentUserId());
        }
        return buildTenantSummary(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long id) {
        securityUtils.requirePermission(PermissionConstants.TENANT_VIEW);
        return tenantMapper.toResponse(getAccessibleTenant(id));
    }

    @Override
    @Transactional
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        securityUtils.requirePermission(PermissionConstants.TENANT_UPDATE);
        Tenant tenant = getAccessibleTenant(id);

        if (request.phone() != null) {
            if (tenantRepository.existsByOwnerIdAndPhoneAndIdNot(tenant.getOwner().getId(),
                    request.phone(), tenant.getId())) {
                throw new DuplicateResourceException(
                        "Tenant phone already exists for this owner: " + request.phone());
            }
            tenant.setPhone(request.phone());
        }
        if (request.firstName() != null) {
            tenant.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            tenant.setLastName(request.lastName());
        }
        if (request.gender() != null) {
            tenant.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            tenant.setDateOfBirth(request.dateOfBirth());
        }
        if (request.email() != null) {
            tenant.setEmail(request.email());
        }
        if (request.occupationType() != null) {
            tenant.setOccupationType(request.occupationType());
        }
        if (request.occupation() != null) {
            tenant.setOccupation(request.occupation());
        }
        if (request.schoolOrCompany() != null) {
            tenant.setSchoolOrCompany(request.schoolOrCompany());
        }
        if (request.idType() != null) {
            tenant.setIdType(request.idType());
        }
        if (request.idNumber() != null) {
            tenant.setIdNumber(request.idNumber());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public void deactivateTenant(Long id) {
        securityUtils.requirePermission(PermissionConstants.TENANT_MANAGE);
        Tenant tenant = getAccessibleTenant(id);
        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }

    private Tenant getAccessibleTenant(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> TenantNotFoundException.forId(id));

        boolean isOwner = tenant.getOwner().getId().equals(currentUserId);

        if (isOwner) {
            return tenant;
        }

        if (tenant.getHouse() != null && houseAccessService.canAccessHouse(tenant.getHouse().getId())) {
            return tenant;
        }

        throw TenantNotFoundException.forId(id);
    }

    private void validateUniqueFields(Long ownerId, String tenantCode, String phone, Long tenantId) {
        boolean isCreate = tenantId == null;

        if (tenantCode != null && (isCreate
                ? tenantRepository.existsByOwnerIdAndTenantCode(ownerId, tenantCode)
                : tenantRepository.existsByOwnerIdAndTenantCodeAndIdNot(ownerId, tenantCode, tenantId))) {
            throw new DuplicateResourceException(
                    "Tenant code already exists for this owner: " + tenantCode);
        }
        if (phone != null && (isCreate
                ? tenantRepository.existsByOwnerIdAndPhone(ownerId, phone)
                : tenantRepository.existsByOwnerIdAndPhoneAndIdNot(ownerId, phone, tenantId))) {
            throw new DuplicateResourceException(
                    "Tenant phone already exists for this owner: " + phone);
        }
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }

    /**
     * A linked user account must be inside the caller's authorized scope. A
     * SUPER_ADMIN is unrestricted; an OWNER or MANAGER may only link a user they
     * can manage, reusing the same ownership scope as user management (see
     * {@code HouseAccessService}). Owner attribution itself is never taken from
     * the request.
     */
    private void requireUserWithinCallerScope(Long userId) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return;
        }
        Long selfId = securityUtils.getCurrentUserId();
        if (selfId != null && selfId.equals(userId)) {
            return;
        }
        boolean inScope = userRepository.existsInOwnerScope(userId, selfId,
                houseAccessService.getScopedOwnerIds(),
                houseAccessService.getAccessibleHouseIds());
        if (!inScope) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private Specification<Tenant> tenantFilters(Long ownerId, List<Long> houseIds, String q, TenantStatus status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            if (ownerId != null) {
                preds.add(cb.equal(root.get("owner").get("id"), ownerId));
            } else {
                preds.add(root.get("house").get("id").in(houseIds));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("tenantCode")), like),
                        cb.like(cb.lower(root.get("phone")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private TenantSummaryResponse buildTenantSummary(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0L;
        for (Object[] row : rows) {
            String status = row[0].toString();
            long cnt = ((Number) row[1]).longValue();
            counts.put(status, cnt);
            total += cnt;
        }
        return new TenantSummaryResponse(total, counts);
    }

    private static String normalize(String q) {
        return q == null ? null : q.trim();
    }
}
