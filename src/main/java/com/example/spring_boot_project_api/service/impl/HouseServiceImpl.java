package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.HouseRoomCountProjection;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final HouseImageRepository houseImageRepository;
    private final RoomRepository roomRepository;
    private final RentalContractRepository rentalContractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final TenantRepository tenantRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final TenantMapper tenantMapper;
    private final RentalContractMapper rentalContractMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public HouseResponse createHouse(CreateHouseRequest request) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_CREATE);
        requireOwner();

        Long ownerId = securityUtils.getCurrentUserId();

        validateUniqueFields(ownerId, request.name(), request.slug(), request.code(), null);

        House house = houseMapper.toEntity(request);
        house.setOwner(securityUtils.getCurrentUser());

        // Room counts are derived from the actual rooms table, never from input.
        house.setTotalRooms(0);
        house.setAvailableRooms(0);

        House saved = houseRepository.save(house);
        return houseMapper.toResponse(saved).withRoomCounts(0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public HouseResponse getHouseById(Long id) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        House house = requireOwnerOrManager(id);
        return withDerivedRoomCounts(house, primaryImageUrl(house.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getMyHouses() {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        Long userId = securityUtils.getCurrentUserId();

        // OWNER always sees all houses within its ownership scope.
        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return toResponsesWithPrimaryImages(
                    houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId));
        }

        // MANAGER sees only the houses assigned through user_house_access.
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            return toResponsesWithPrimaryImages(userHouseAccessRepository.findHousesByUserId(userId));
        }

        return List.of();
    }

    private List<HouseResponse> toResponsesWithPrimaryImages(List<House> houses) {
        if (houses.isEmpty()) {
            return List.of();
        }
        List<Long> houseIds = houses.stream().map(House::getId).toList();
        Map<Long, String> primaryByHouseId = houseImageRepository.findPrimaryByHouseIds(houseIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getHouse().getId(),
                        HouseImage::getSecureUrl,
                        (first, ignored) -> first));

        Map<Long, Long> totalByHouseId = new HashMap<>();
        Map<Long, Long> availableByHouseId = new HashMap<>();
        for (HouseRoomCountProjection count : roomRepository.countRoomsByHouseIds(houseIds)) {
            totalByHouseId.put(count.getHouseId(), count.getRoomCount());
        }
        for (HouseRoomCountProjection count : roomRepository.countRoomsByHouseIdsAndStatus(
                houseIds, RoomStatus.AVAILABLE)) {
            availableByHouseId.put(count.getHouseId(), count.getRoomCount());
        }

        return houses.stream()
                .map(house -> houseMapper.toResponse(house, primaryByHouseId.get(house.getId()))
                        .withRoomCounts(
                                totalByHouseId.getOrDefault(house.getId(), 0L).intValue(),
                                availableByHouseId.getOrDefault(house.getId(), 0L).intValue()))
                .toList();
    }

    /**
     * Maps the house to a response and replaces the persisted room counts with
     * the values calculated from the actual {@code rooms} records.
     */
    private HouseResponse withDerivedRoomCounts(House house, String primaryImage) {
        int totalRooms = Math.toIntExact(roomRepository.countByHouseId(house.getId()));
        int availableRooms = Math.toIntExact(
                roomRepository.countByHouseIdAndStatus(house.getId(), RoomStatus.AVAILABLE));
        return houseMapper.toResponse(house, primaryImage).withRoomCounts(totalRooms, availableRooms);
    }

    private String primaryImageUrl(Long houseId) {
        return houseImageRepository.findFirstByHouseIdAndIsPrimaryTrue(houseId)
                .map(HouseImage::getSecureUrl)
                .orElse(null);
    }

    @Override
    @Transactional
    public HouseResponse updateHouse(Long id, UpdateHouseRequest request) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_UPDATE);
        requireOwner();
        House house = getOwnedHouse(id);
        Long ownerId = house.getOwner().getId();

        validateUniqueFields(ownerId, request.name(), request.slug(), request.code(), house.getId());

        if (request.name() != null) {
            house.setName(request.name());
        }
        if (request.slug() != null) {
            house.setSlug(request.slug());
        }
        if (request.code() != null) {
            house.setCode(request.code());
        }
        if (request.address() != null) {
            house.setAddress(request.address());
        }
        if (request.postalCode() != null) {
            house.setPostalCode(request.postalCode());
        }
        if (request.latitude() != null) {
            house.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            house.setLongitude(request.longitude());
        }
        if (request.cityProvince() != null) {
            house.setCityProvince(request.cityProvince());
        }
        if (request.googleMapsUrl() != null) {
            house.setGoogleMapsUrl(request.googleMapsUrl());
        }
        // totalRooms/availableRooms are read-only derived values; any values sent
        // by the client are intentionally ignored and recalculated from rooms.
        if (request.description() != null) {
            house.setDescription(request.description());
        }
        if (request.status() != null) {
            house.setStatus(request.status());
        }
        if (request.propertyType() != null) {
            house.setPropertyType(request.propertyType());
        }
        if (request.furnishing() != null) {
            house.setFurnishing(request.furnishing());
        }
        if (request.availableFrom() != null) {
            house.setAvailableFrom(request.availableFrom());
        }
        if (request.advancePayment() != null) {
            house.setAdvancePayment(request.advancePayment());
        }
        if (request.bedrooms() != null) {
            house.setBedrooms(request.bedrooms());
        }
        if (request.bathrooms() != null) {
            house.setBathrooms(request.bathrooms());
        }
        if (request.areaSqm() != null) {
            house.setAreaSqm(request.areaSqm());
        }
        if (request.landSizeSqm() != null) {
            house.setLandSizeSqm(request.landSizeSqm());
        }
        if (request.numberOfFloors() != null) {
            house.setNumberOfFloors(request.numberOfFloors());
        }

        return withDerivedRoomCounts(houseRepository.save(house), null);
    }

    @Override
    @Transactional
    public void deleteHouse(Long id) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_DELETE);
        requireOwner();
        House house = getOwnedHouse(id);
        houseRepository.delete(house);
    }

    @Override
    @Transactional(readOnly = true)
    public HouseSummaryResponse getHouseSummary(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        Long ownerOfHouse = requireOwnerOrManager(houseId).getOwner().getId();
        boolean isOwner = ownerOfHouse.equals(securityUtils.getCurrentUserId());

        long totalRooms = roomRepository.countByHouseId(houseId);
        long availableRooms = roomRepository.countByHouseIdAndStatus(houseId, RoomStatus.AVAILABLE);
        long occupiedRooms = roomRepository.countByHouseIdAndStatus(houseId, RoomStatus.OCCUPIED);
        long maintenanceRooms = roomRepository.countByHouseIdAndStatus(houseId, RoomStatus.MAINTENANCE);

        long activeTenants;
        long activeContracts;
        if (isOwner) {
            Long ownerId = securityUtils.getCurrentUserId();
            activeTenants = contractTenantRepository.countActiveTenantsByHouseIdAndOwnerId(
                    houseId, ownerId, LocalDate.now());
            activeContracts = rentalContractRepository.countByHouseIdAndOwnerIdAndStatus(
                    houseId, ownerId, RentalContractStatus.ACTIVE);
        } else {
            activeTenants = contractTenantRepository.countActiveTenantsByHouseId(houseId, LocalDate.now());
            activeContracts = rentalContractRepository.countByHouseIdAndStatus(houseId, RentalContractStatus.ACTIVE);
        }

        return new HouseSummaryResponse(
                totalRooms,
                availableRooms,
                occupiedRooms,
                maintenanceRooms,
                activeTenants,
                activeContracts);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getHouseRooms(Long houseId, String search, RoomStatus status,
            Long roomTypeId, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        boolean isOwner = requireOwnerOrManager(houseId).getOwner().getId()
                .equals(securityUtils.getCurrentUserId());

        Page<Room> page;
        if (isOwner) {
            Long ownerId = securityUtils.getCurrentUserId();
            page = roomRepository.findByHouseIdWithFilters(
                    houseId, ownerId, search, status, roomTypeId, pageable);
        } else {
            page = roomRepository.findByHouseIdWithFilters(houseId, search, status, roomTypeId, pageable);
        }

        List<RoomResponse> content = page.getContent().stream()
                .map(room -> roomMapper.toDto(room, null))
                .toList();

        return toPageResponse(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> getHouseTenants(Long houseId, String search, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        boolean isOwner = requireOwnerOrManager(houseId).getOwner().getId()
                .equals(securityUtils.getCurrentUserId());

        Page<Tenant> page;
        if (isOwner) {
            Long ownerId = securityUtils.getCurrentUserId();
            page = tenantRepository.findActiveTenantsByHouseId(
                    houseId, ownerId, RentalContractStatus.ACTIVE, LocalDate.now(), search, pageable);
        } else {
            page = tenantRepository.findActiveTenantsByHouseId(
                    houseId, RentalContractStatus.ACTIVE, LocalDate.now(), search, pageable);
        }

        List<TenantResponse> content = page.getContent().stream()
                .map(tenantMapper::toResponse)
                .toList();

        return toPageResponse(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalContractResponse> getHouseContracts(Long houseId, String search,
            RentalContractStatus status, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        boolean isOwner = requireOwnerOrManager(houseId).getOwner().getId()
                .equals(securityUtils.getCurrentUserId());

        Page<RentalContract> page;
        if (isOwner) {
            Long ownerId = securityUtils.getCurrentUserId();
            page = rentalContractRepository.findByHouseIdWithFilters(
                    houseId, ownerId, search, status, pageable);
        } else {
            page = rentalContractRepository.findByHouseIdWithFilters(houseId, search, status, pageable);
        }

        List<RentalContractResponse> content = page.getContent().stream()
                .map(rentalContractMapper::toResponse)
                .toList();

        return toPageResponse(content, page);
    }

    /** Returns the house if the current user is the owner or an assigned manager, else denies. */
    private House requireOwnerOrManager(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));

        if (house.getOwner().getId().equals(currentUserId)) {
            return house;
        }

        if (userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
            return house;
        }

        throw new AccessDeniedException("You do not have permission to access this resource");
    }

    private House getOwnedHouse(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();

        return houseRepository.findById(id)
                .map(house -> {
                    if (!house.getOwner().getId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return house;
                })
                .orElseThrow(() -> HouseNotFoundException.forId(id));
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private void validateUniqueFields(Long ownerId, String name, String slug, String code, Long houseId) {
        boolean isCreate = houseId == null;

        if (name != null && (isCreate
                ? houseRepository.existsByOwnerIdAndName(ownerId, name)
                : houseRepository.existsByOwnerIdAndNameAndIdNot(ownerId, name, houseId))) {
            throw new DuplicateResourceException("House name already exists for this owner: " + name);
        }
        if (slug != null && (isCreate
                ? houseRepository.existsByOwnerIdAndSlug(ownerId, slug)
                : houseRepository.existsByOwnerIdAndSlugAndIdNot(ownerId, slug, houseId))) {
            throw new DuplicateResourceException("House slug already exists for this owner: " + slug);
        }
        if (code != null && (isCreate
                ? houseRepository.existsByOwnerIdAndCode(ownerId, code)
                : houseRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, code, houseId))) {
            throw new DuplicateResourceException("House code already exists for this owner: " + code);
        }
    }

    private <T> PageResponse<T> toPageResponse(List<T> content, Page<?> page) {
        return PageResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
