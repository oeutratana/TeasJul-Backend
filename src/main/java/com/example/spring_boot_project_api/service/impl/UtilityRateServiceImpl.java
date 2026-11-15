package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityRateResponse;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityRateNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityRateMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.UtilityRateService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityRateServiceImpl implements UtilityRateService {

    private final UtilityRateRepository rateRepository;
    private final HouseRepository houseRepository;
    private final UtilityTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final UtilityRateMapper rateMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public UtilityRateResponse createRate(CreateUtilityRateRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        User owner = getCurrentOwner();

        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> HouseNotFoundException.forId(request.houseId()));
            owner = house.getOwner();
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            if (!approvedHouseIds.contains(request.houseId())) {
                throw HouseNotFoundException.forId(request.houseId());
            }
            House house = houseRepository.findById(request.houseId())
                    .orElseThrow(() -> HouseNotFoundException.forId(request.houseId()));
            owner = house.getOwner();
        }

        validateReferences(request.houseId(), request.utilityTypeId(), owner.getId());
        validateDates(request.effectiveFrom(), request.effectiveTo());
        validateRate(request.rate());

        assertNoOverlap(owner.getId(), request.houseId(), request.utilityTypeId(),
                request.effectiveFrom(), request.effectiveTo(), null);

        UtilityRate rate = rateMapper.toEntity(request);
        rate.setOwner(owner);

        return rateMapper.toResponse(rateRepository.save(rate));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UtilityRateResponse> getRates(Long houseId, Long utilityTypeId,
            UtilityRateStatus status, LocalDate effectiveFrom, LocalDate effectiveTo,
            Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        Long currentOwnerId = getCurrentOwner().getId();
        List<Long> scopedOwnerIds;
        final List<Long> scopedHouseIds;

        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            scopedOwnerIds = null;
            scopedHouseIds = houseId == null ? null : List.of(houseId);
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            scopedOwnerIds = new ArrayList<>(userHouseAccessRepository.findOwnerIdsByUserId(securityUtils.getCurrentUserId()));
            if (!scopedOwnerIds.contains(currentOwnerId)) {
                scopedOwnerIds.add(currentOwnerId);
            }
            scopedHouseIds = resolveApprovedHouseIds(houseId);
        } else {
            scopedOwnerIds = List.of(currentOwnerId);
            scopedHouseIds = null;
        }

        Specification<UtilityRate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (scopedOwnerIds != null) {
                predicates.add(root.get("owner").get("id").in(scopedOwnerIds));
            }
            if (scopedHouseIds != null) {
                predicates.add(root.get("houseId").in(scopedHouseIds));
            } else if (houseId != null) {
                predicates.add(cb.equal(root.get("houseId"), houseId));
            }
            if (utilityTypeId != null) {
                predicates.add(cb.equal(root.get("utilityTypeId"), utilityTypeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (effectiveFrom != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectiveFrom"), effectiveTo == null
                        ? effectiveFrom : effectiveTo));
            }
            if (effectiveTo != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("effectiveTo")),
                        cb.greaterThanOrEqualTo(root.get("effectiveTo"), effectiveFrom)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(rateRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityRateResponse getRateById(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        return rateMapper.toResponse(getOwnedRate(id));
    }

    @Override
    @Transactional
    public UtilityRateResponse updateRate(Long id, UpdateUtilityRateRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityRate rate = getOwnedRate(id);

        Long effectiveHouseId = request.houseId() != null ? request.houseId() : rate.getHouseId();
        Long effectiveUtilityTypeId = request.utilityTypeId() != null
                ? request.utilityTypeId() : rate.getUtilityTypeId();
        LocalDate effectiveFrom = request.effectiveFrom() != null
                ? request.effectiveFrom() : rate.getEffectiveFrom();
        LocalDate effectiveTo = request.effectiveTo() != null
                ? request.effectiveTo() : rate.getEffectiveTo();
        BigDecimal effectiveRate = request.rate() != null ? request.rate() : rate.getRate();

        validateReferences(effectiveHouseId, effectiveUtilityTypeId, rate.getOwner().getId());
        validateDates(effectiveFrom, effectiveTo);
        validateRate(effectiveRate);

        assertNoOverlap(rate.getOwner().getId(), effectiveHouseId, effectiveUtilityTypeId,
                effectiveFrom, effectiveTo, rate.getId());

        rate.setHouseId(effectiveHouseId);
        rate.setUtilityTypeId(effectiveUtilityTypeId);
        rate.setRate(effectiveRate);
        rate.setEffectiveFrom(effectiveFrom);
        rate.setEffectiveTo(effectiveTo);
        if (request.status() != null) {
            rate.setStatus(request.status());
        }

        return rateMapper.toResponse(rateRepository.save(rate));
    }

    @Override
    @Transactional
    public void deactivateRate(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityRate rate = getOwnedRate(id);
        rate.setStatus(UtilityRateStatus.INACTIVE);
        rateRepository.save(rate);
    }

    private void assertNoOverlap(Long ownerId, Long houseId, Long utilityTypeId,
            LocalDate effectiveFrom, LocalDate effectiveTo, Long excludeId) {
        List<UtilityRate> existing = excludeId == null
                ? rateRepository.findByOwnerIdAndHouseIdAndUtilityTypeId(ownerId, houseId, utilityTypeId)
                : rateRepository.findByOwnerIdAndHouseIdAndUtilityTypeIdAndIdNot(
                        ownerId, houseId, utilityTypeId, excludeId);

        for (UtilityRate candidate : existing) {
            if (overlaps(effectiveFrom, effectiveTo,
                    candidate.getEffectiveFrom(), candidate.getEffectiveTo())) {
                throw new InvalidUtilityOperationException(
                        "Rate period overlaps with an existing rate for the same house and utility type");
            }
        }
    }

    private boolean overlaps(LocalDate from1, LocalDate to1, LocalDate from2, LocalDate to2) {
        LocalDate end1 = to1 != null ? to1 : LocalDate.MAX;
        LocalDate end2 = to2 != null ? to2 : LocalDate.MAX;
        return !from1.isAfter(end2) && !from2.isAfter(end1);
    }

    private void validateReferences(Long houseId, Long utilityTypeId, Long ownerId) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            if (!approvedHouseIds.contains(houseId)) {
                throw HouseNotFoundException.forId(houseId);
            }
            House house = houseRepository.findById(houseId)
                    .orElseThrow(() -> HouseNotFoundException.forId(houseId));
            Long houseOwnerId = house.getOwner().getId();
            typeRepository.findByIdAndOwnerId(utilityTypeId, houseOwnerId)
                    .orElseThrow(() -> UtilityTypeNotFoundException.forId(utilityTypeId));
        } else {
            houseRepository.findByIdAndOwnerId(houseId, ownerId)
                    .orElseThrow(() -> HouseNotFoundException.forId(houseId));

            typeRepository.findByIdAndOwnerId(utilityTypeId, ownerId)
                    .orElseThrow(() -> UtilityTypeNotFoundException.forId(utilityTypeId));
        }
    }

    private void validateDates(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new InvalidUtilityOperationException(
                    "Effective to must not be before effective from");
        }
    }

    private void validateRate(BigDecimal rate) {
        if (rate != null && rate.signum() < 0) {
            throw new InvalidUtilityOperationException("Rate must not be negative");
        }
    }

    private UtilityRate getOwnedRate(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        UtilityRate rate = rateRepository.findById(id)
                .orElseThrow(() -> UtilityRateNotFoundException.forId(id));

        Long rateOwnerId = rate.getOwner().getId();
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return rate;
        }
        if (rateOwnerId.equals(getCurrentOwner().getId())) {
            return rate;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (approvedHouseIds.contains(rate.getHouseId())) {
                return rate;
            }
        }

        throw UtilityRateNotFoundException.forId(id);
    }

    private List<Long> resolveApprovedHouseIds(Long requestedHouseId) {
        List<Long> assigned = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
        if (assigned.isEmpty()) {
            return List.of(-1L);
        }
        if (requestedHouseId != null && !assigned.contains(requestedHouseId)) {
            return List.of(-1L);
        }
        return requestedHouseId != null ? List.of(requestedHouseId) : assigned;
    }

    private PageResponse<UtilityRateResponse> toPageResponse(Page<UtilityRate> page) {
        List<UtilityRateResponse> content = page.getContent().stream()
                .map(rateMapper::toResponse)
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
