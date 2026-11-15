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

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityMeterNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityMeterMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.UtilityMeterService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityMeterServiceImpl implements UtilityMeterService {

    private final UtilityMeterRepository meterRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final UtilityTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final UtilityMeterMapper meterMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public UtilityMeterResponse createMeter(CreateUtilityMeterRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        User owner = getCurrentOwner();

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            if (!approvedHouseIds.contains(request.houseId())) {
                throw HouseNotFoundException.forId(request.houseId());
            }
            House house = houseRepository.findById(request.houseId())
                    .orElseThrow(() -> HouseNotFoundException.forId(request.houseId()));
            owner = house.getOwner();
        }

        validateReferences(request.houseId(), request.roomId(), request.utilityTypeId(), owner.getId());

        assertNoActiveMeterForRoomType(request.roomId(), request.utilityTypeId(), null);

        if (meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(
                owner.getId(), request.utilityTypeId(), request.meterNumber())) {
            throw new DuplicateResourceException(
                    "Meter number already exists: " + request.meterNumber());
        }

        UtilityMeter meter = meterMapper.toEntity(request);
        meter.setOwner(owner);

        return meterMapper.toResponse(meterRepository.save(meter));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UtilityMeterResponse> getMeters(String search, Long houseId, Long roomId,
            Long utilityTypeId, UtilityMeterStatus status, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();

        Specification<UtilityMeter> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("houseId").in(accessibleHouseIds));
            if (houseId != null) {
                predicates.add(cb.equal(root.get("houseId"), houseId));
            }
            if (roomId != null) {
                predicates.add(cb.equal(root.get("roomId"), roomId));
            }
            if (utilityTypeId != null) {
                predicates.add(cb.equal(root.get("utilityTypeId"), utilityTypeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("meterNumber")),
                        "%" + search.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(meterRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityMeterResponse getMeterById(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        return meterMapper.toResponse(getOwnedMeter(id));
    }

    @Override
    @Transactional
    public UtilityMeterResponse updateMeter(Long id, UpdateUtilityMeterRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityMeter meter = getOwnedMeter(id);

        Long effectiveHouseId = request.houseId() != null ? request.houseId() : meter.getHouseId();
        Long effectiveRoomId = request.roomId() != null ? request.roomId() : meter.getRoomId();
        Long effectiveUtilityTypeId = request.utilityTypeId() != null
                ? request.utilityTypeId() : meter.getUtilityTypeId();

        validateReferences(effectiveHouseId, effectiveRoomId, effectiveUtilityTypeId,
                meter.getOwner().getId());

        String effectiveMeterNumber = request.meterNumber() != null
                ? request.meterNumber() : meter.getMeterNumber();
        if (meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumberAndIdNot(
                meter.getOwner().getId(), effectiveUtilityTypeId, effectiveMeterNumber, meter.getId())) {
            throw new DuplicateResourceException("Meter number already exists: " + effectiveMeterNumber);
        }

        assertNoActiveMeterForRoomType(effectiveRoomId, effectiveUtilityTypeId, meter.getId());

        meter.setHouseId(effectiveHouseId);
        meter.setRoomId(effectiveRoomId);
        meter.setUtilityTypeId(effectiveUtilityTypeId);
        meter.setMeterNumber(effectiveMeterNumber);
        if (request.installedAt() != null) {
            meter.setInstalledAt(request.installedAt());
        }
        if (request.status() != null) {
            meter.setStatus(request.status());
        }

        return meterMapper.toResponse(meterRepository.save(meter));
    }

    @Override
    @Transactional
    public void deactivateMeter(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_UPDATE);
        UtilityMeter meter = getOwnedMeter(id);
        meter.setStatus(UtilityMeterStatus.INACTIVE);
        meterRepository.save(meter);
    }

    private void validateReferences(Long houseId, Long roomId, Long utilityTypeId, Long ownerId) {
        House house;
        Long resolvedOwnerId;

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            if (!approvedHouseIds.contains(houseId)) {
                throw HouseNotFoundException.forId(houseId);
            }
            house = houseRepository.findById(houseId)
                    .orElseThrow(() -> HouseNotFoundException.forId(houseId));
            resolvedOwnerId = house.getOwner().getId();
        } else {
            house = houseRepository.findByIdAndOwnerId(houseId, ownerId)
                    .orElseThrow(() -> HouseNotFoundException.forId(houseId));
            resolvedOwnerId = ownerId;
        }

        UtilityType type = typeRepository.findByIdAndOwnerId(utilityTypeId, resolvedOwnerId)
                .orElseThrow(() -> UtilityTypeNotFoundException.forId(utilityTypeId));
        if (type.getBillingMethod() != com.example.spring_boot_project_api.enums.UtilityBillingMethod.METERED) {
            throw new InvalidUtilityOperationException(
                    "A meter can only be created for a METERED utility type "
                            + "(type " + type.getCode() + " is FIXED)");
        }

        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new InvalidUtilityOperationException(
                            "Room not found with id: " + roomId));
            if (!room.getOwnerId().equals(resolvedOwnerId)) {
                throw new InvalidUtilityOperationException(
                        "Room " + roomId + " does not belong to this owner");
            }
            if (!room.getHouseId().equals(house.getId())) {
                throw new InvalidUtilityOperationException(
                        "Room " + roomId + " does not belong to house " + house.getId());
            }
        }
    }

    private UtilityMeter getOwnedMeter(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        UtilityMeter meter = meterRepository.findById(id)
                .orElseThrow(() -> UtilityMeterNotFoundException.forId(id));

        Long meterOwnerId = meter.getOwner().getId();
        if (meterOwnerId.equals(getCurrentOwner().getId())) {
            return meter;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (approvedHouseIds.contains(meter.getHouseId())) {
                return meter;
            }
        }

        throw UtilityMeterNotFoundException.forId(id);
    }

    private void assertNoActiveMeterForRoomType(Long roomId, Long utilityTypeId, Long excludeId) {
        if (roomId == null || utilityTypeId == null) return;
        List<UtilityMeter> existing = meterRepository.findByRoomIdAndUtilityTypeId(roomId, utilityTypeId);
        for (UtilityMeter m : existing) {
            if (m.getId().equals(excludeId)) continue;
            if (m.getStatus() == UtilityMeterStatus.ACTIVE) {
                throw new DuplicateResourceException(
                        "This room already has an active meter of this utility type");
            }
        }
    }

    private PageResponse<UtilityMeterResponse> toPageResponse(Page<UtilityMeter> page) {
        List<UtilityMeterResponse> content = page.getContent().stream()
                .map(meterMapper::toResponse)
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
