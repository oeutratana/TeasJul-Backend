package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AdminPropertyHouseRequest;
import com.example.spring_boot_project_api.dto.request.AdminPropertyPostRequest;
import com.example.spring_boot_project_api.dto.request.AdminPropertyUtilityRequest;
import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.PropertyOwnerRequest;
import com.example.spring_boot_project_api.dto.response.AdminCatalogResponse;
import com.example.spring_boot_project_api.dto.response.AdminPropertyResponse;
import com.example.spring_boot_project_api.dto.response.AdminPropertyUtilityResponse;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.PropertyOwnerNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.PropertyOwnerMapper;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.model.PropertyOwner;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomAmenity;
import com.example.spring_boot_project_api.model.RoomAmenityId;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.repository.PropertyOwnerRepository;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.AdminPropertyService;
import com.example.spring_boot_project_api.util.PropertyTypeRules;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * ADMIN/SUPER_ADMIN property-posting workflow, fully separate from the OWNER
 * portal. An admin composes a complete property (house / villa / apartment /
 * condo / room) on behalf of an owner which may be a TeasJul OWNER account or
 * a plain {@link PropertyOwner} record with no login.
 *
 * <p>Access rules (resolved server-side, never trusted from the client):</p>
 * <ul>
 *   <li>Every operation requires the SUPER_ADMIN role, guarded by an explicit
 *       role check (never the generic permission check, because OWNER is
 *       implicitly granted every permission). TENANT and OWNER are therefore
 *       rejected.</li>
 *   <li>Publishing preserves the existing admin rule: a posting SUPER_ADMIN may
 *       publish a DRAFT directly (self-approval). No owner login is required.</li>
 *   <li>No TeasJul {@link User} account is ever created for a {@link PropertyOwner}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminPropertyServiceImpl implements AdminPropertyService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private static final int MAX_LIST_SIZE = 200;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final PropertyOwnerMapper propertyOwnerMapper;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomUtilityRepository roomUtilityRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityRateRepository utilityRateRepository;
    private final NearbyLocationRepository nearbyLocationRepository;
    private final HouseImageRepository houseImageRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPropertyResponse> list(String search, PropertyType propertyType,
            RoomPublicationStatus status, Pageable pageable) {
        requireSuperAdmin();
        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE));
        Page<Room> page = roomRepository.findAdminProperties(trim(search), propertyType, status, capped);
        List<AdminPropertyResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPropertyResponse get(Long roomId) {
        requireSuperAdmin();
        Room room = findRoom(roomId);
        return toResponse(room);
    }

    @Override
    @Transactional
    public AdminPropertyResponse create(AdminPropertyPostRequest request) {
        requireSuperAdmin();
        PropertyType type = requirePropertyType(request.getPropertyType());
        ResolvedOwner owner = resolveOwner(request);
        House house = resolveHouse(owner, request, type);
        RoomType roomType = isRoomLike(type) ? resolveRoomType(owner, request) : null;

        Room room = new Room();
        applyRoom(room, type, house, roomType, owner, request);
        room = roomRepository.save(room);

        replaceRoomAmenities(room.getId(), request.getAmenityIds());
        replaceRoomUtilities(room.getId(), house.getId(), owner, request.getUtilities());
        replaceNearbyLocations(house, request.getNearbyLocations());
        return toResponse(room);
    }

    @Override
    @Transactional
    public AdminPropertyResponse update(Long roomId, AdminPropertyPostRequest request) {
        requireSuperAdmin();
        Room room = findRoom(roomId);
        PropertyType type = requirePropertyType(request.getPropertyType());
        ResolvedOwner owner = resolveOwner(request);
        House house = resolveHouseForUpdate(room, request, owner, type);
        RoomType roomType = isRoomLike(type) ? resolveRoomType(owner, request) : null;

        applyRoom(room, type, house, roomType, owner, request);
        room = roomRepository.save(room);

        replaceRoomAmenities(room.getId(), request.getAmenityIds());
        replaceRoomUtilities(room.getId(), house.getId(), owner, request.getUtilities());
        replaceNearbyLocations(house, request.getNearbyLocations());
        return toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCatalogResponse getCatalog() {
        requireSuperAdmin();
        List<AdminCatalogResponse.AmenityOption> amenities = amenityRepository.findAll().stream()
                .filter(a -> a.getStatus() == AmenityStatus.ACTIVE)
                .map(a -> new AdminCatalogResponse.AmenityOption(a.getId(), a.getName(), a.getCode()))
                .toList();
        List<AdminCatalogResponse.UtilityTypeOption> utilityTypes = utilityTypeRepository.findAll().stream()
                .filter(u -> u.getStatus() == UtilityTypeStatus.ACTIVE)
                .map(u -> new AdminCatalogResponse.UtilityTypeOption(u.getId(), u.getName(), u.getUnit()))
                .toList();
        List<PropertyOwnerResponse> propertyOwners = propertyOwnerRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(propertyOwnerMapper::toResponse)
                .toList();
        List<UserResponse> ownerAccounts = userRepository
                .findAllOwners(null, null, PageRequest.of(0, MAX_LIST_SIZE))
                .getContent()
                .stream()
                .map(userMapper::toResponse)
                .toList();
        return new AdminCatalogResponse(amenities, utilityTypes, propertyOwners, ownerAccounts);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropertyOwnerResponse> listPropertyOwners(String search, Pageable pageable) {
        requireSuperAdmin();
        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE));
        Page<PropertyOwner> page = (search == null || search.isBlank())
                ? propertyOwnerRepository.findAll(capped)
                : propertyOwnerRepository.findByNameContainingIgnoreCase(trim(search), capped);
        List<PropertyOwnerResponse> content = page.getContent().stream()
                .map(propertyOwnerMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public PropertyOwnerResponse createPropertyOwner(PropertyOwnerRequest request) {
        requireSuperAdmin();
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }
        PropertyOwner record = propertyOwnerRepository.save(propertyOwnerMapper.toEntity(request));
        return propertyOwnerMapper.toResponse(record);
    }

    @Override
    @Transactional
    public void deletePropertyOwner(Long id) {
        requireSuperAdmin();
        PropertyOwner record = propertyOwnerRepository.findById(id)
                .orElseThrow(() -> PropertyOwnerNotFoundException.forId(id));
        if (roomRepository.existsByOwnerRecordId(id) || houseRepository.existsByOwnerRecordId(id)) {
            throw new IllegalArgumentException(
                    "This owner is attached to one or more properties and cannot be deleted");
        }
        propertyOwnerRepository.delete(record);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    private PropertyType requirePropertyType(PropertyType type) {
        if (type == null) {
            throw new IllegalArgumentException("Property type is required");
        }
        return type;
    }

    private static boolean isRoomLike(PropertyType type) {
        return PropertyTypeRules.isRoomLike(type);
    }

    /**
     * Resolves exactly one owner: a TeasJul OWNER account, an existing
     * {@link PropertyOwner} record, or an inline record (persisted here). A
     * {@link User} is never created.
     */
    private ResolvedOwner resolveOwner(AdminPropertyPostRequest request) {
        int ways = 0;
        if (request.getOwnerUserId() != null) {
            ways++;
        }
        if (request.getOwnerRecordId() != null) {
            ways++;
        }
        if (request.getOwnerRecord() != null) {
            ways++;
        }
        if (ways != 1) {
            throw new IllegalArgumentException(
                    "Specify exactly one owner: ownerUserId, ownerRecordId or ownerRecord");
        }
        if (request.getOwnerUserId() != null) {
            User user = userRepository.findById(request.getOwnerUserId())
                    .orElseThrow(() -> UserNotFoundException.forId(request.getOwnerUserId()));
            if (!userRoleRepository.existsByUserIdAndRoleCode(user.getId(), SecurityUtils.ROLE_OWNER)) {
                throw UserNotFoundException.forId(request.getOwnerUserId());
            }
            return new ResolvedOwner(user, null);
        }
        if (request.getOwnerRecordId() != null) {
            PropertyOwner record = propertyOwnerRepository.findById(request.getOwnerRecordId())
                    .orElseThrow(() -> PropertyOwnerNotFoundException.forId(request.getOwnerRecordId()));
            return new ResolvedOwner(null, record);
        }
        PropertyOwner record = propertyOwnerMapper.toEntity(request.getOwnerRecord());
        return new ResolvedOwner(null, propertyOwnerRepository.save(record));
    }

    private House resolveHouse(ResolvedOwner owner, AdminPropertyPostRequest request, PropertyType type) {
        if (request.getHouseId() != null) {
            return houseRepository.findById(request.getHouseId())
                    .orElseThrow(() -> HouseNotFoundException.forId(request.getHouseId()));
        }
        return createHouse(owner, request.getHouse(), type);
    }

    private House resolveHouseForUpdate(Room room, AdminPropertyPostRequest request,
            ResolvedOwner owner, PropertyType type) {
        Long houseId = request.getHouseId() != null ? request.getHouseId() : room.getHouseId();
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
        if (request.getHouse() != null) {
            applyHouse(house, request.getHouse(), type);
            house.setOwner(owner.user);
            house.setOwnerRecordId(owner.record != null ? owner.record.getId() : null);
            house = houseRepository.save(house);
        }
        return house;
    }

    private House createHouse(ResolvedOwner owner, AdminPropertyHouseRequest req, PropertyType type) {
        if (req == null) {
            throw new IllegalArgumentException("House or building information is required");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Property name is required");
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        House house = new House();
        house.setOwner(owner.user);
        house.setOwnerRecordId(owner.record != null ? owner.record.getId() : null);
        house.setName(req.getName().trim());
        house.setSlug(slugify(req.getName()) + "-" + randomToken(6));
        house.setCode("PRP-" + randomToken(6).toUpperCase(Locale.ROOT));
        house.setAddress(req.getAddress().trim());
        house.setPostalCode(req.getPostalCode());
        house.setCityProvince(req.getCityProvince());
        house.setDescription(req.getDescription());
        house.setStatus(HouseStatus.ACTIVE);
        house.setPropertyType(req.getPropertyType() != null ? req.getPropertyType() : type);
        house.setFurnishing(req.getFurnishing());
        house.setAdvancePayment(req.getAdvancePayment());
        house.setBedrooms(req.getBedrooms());
        house.setBathrooms(req.getBathrooms());
        house.setAreaSqm(req.getAreaSqm());
        house.setLandSizeSqm(req.getLandSizeSqm());
        house.setNumberOfFloors(req.getNumberOfFloors());
        house.setTotalRooms(0);
        house.setAvailableRooms(0);
        return houseRepository.save(house);
    }

    private void applyHouse(House house, AdminPropertyHouseRequest req, PropertyType type) {
        if (req.getName() != null && !req.getName().isBlank()) {
            house.setName(req.getName().trim());
        }
        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            house.setAddress(req.getAddress().trim());
        }
        house.setPostalCode(req.getPostalCode());
        house.setCityProvince(req.getCityProvince());
        house.setDescription(req.getDescription());
        house.setPropertyType(req.getPropertyType() != null ? req.getPropertyType() : type);
        house.setFurnishing(req.getFurnishing());
        house.setAdvancePayment(req.getAdvancePayment());
        house.setBedrooms(req.getBedrooms());
        house.setBathrooms(req.getBathrooms());
        house.setAreaSqm(req.getAreaSqm());
        house.setLandSizeSqm(req.getLandSizeSqm());
        house.setNumberOfFloors(req.getNumberOfFloors());
    }

    private RoomType resolveRoomType(ResolvedOwner owner, AdminPropertyPostRequest request) {
        if (request.getRoomTypeId() != null) {
            return roomTypeRepository.findById(request.getRoomTypeId())
                    .orElseThrow(() -> RoomTypeNotFoundException.forId(request.getRoomTypeId()));
        }
        if (request.getRoomType() == null) {
            throw new IllegalArgumentException("Room type is required for ROOM listings");
        }
        RoomType roomType = new RoomType();
        roomType.setOwnerId(owner.user != null ? owner.user.getId() : securityUtils.getCurrentUserId());
        roomType.setName(request.getRoomType().getName());
        roomType.setCode("RT-" + randomToken(8).toUpperCase(Locale.ROOT));
        roomType.setDescription(request.getRoomType().getDescription());
        roomType.setCapacity(request.getRoomType().getCapacity() != null
                ? request.getRoomType().getCapacity() : 1);
        roomType.setDefaultRent(request.getRoomType().getDefaultRent() != null
                ? request.getRoomType().getDefaultRent() : BigDecimal.ZERO);
        roomType.setDefaultDeposit(request.getRoomType().getDefaultDeposit() != null
                ? request.getRoomType().getDefaultDeposit() : BigDecimal.ZERO);
        roomType.setStatus(RoomTypeStatus.ACTIVE);
        return roomTypeRepository.save(roomType);
    }

    /**
     * Applies the posted fields onto a {@link Room}, validating the fields that
     * are implied by the selected property type.
     */
    private void applyRoom(Room room, PropertyType type, House house, RoomType roomType,
            ResolvedOwner owner, AdminPropertyPostRequest request) {
        Long effectiveRoomTypeId = roomType != null
                ? roomType.getId()
                : (isRoomLike(type) ? request.getRoomTypeId() : null);
        PropertyTypeRules.validate(type, effectiveRoomTypeId,
                request.getRoomNumber(), request.getFloorNumber(), request.getCapacity());

        room.setOwnerId(owner.user != null ? owner.user.getId() : null);
        room.setOwnerRecordId(owner.record != null ? owner.record.getId() : null);
        room.setHouseId(house.getId());
        room.setRoomTypeId(effectiveRoomTypeId);

        if (PropertyTypeRules.isStructureLike(type)) {
            room.setRoomNumber(null);
            room.setFloorNumber(null);
        } else {
            room.setRoomNumber(request.getRoomNumber());
            room.setFloorNumber(request.getFloorNumber());
        }

        room.setName(request.getName());
        room.setCapacity(request.getCapacity() != null ? request.getCapacity() : 1);
        room.setMonthlyRent(request.getMonthlyRent() != null
                ? request.getMonthlyRent() : BigDecimal.ZERO);
        room.setSecurityDeposit(request.getSecurityDeposit() != null
                ? request.getSecurityDeposit() : BigDecimal.ZERO);
        room.setAdvancePayment(request.getAdvancePayment());
        room.setAvailableFrom(request.getAvailableFrom());
        room.setStatus(request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE);
        room.setFurnishing(request.getFurnishing());
        room.setDescription(request.getDescription());
        room.setBedrooms(request.getBedrooms());
        room.setBathrooms(request.getBathrooms());
        room.setAreaSqm(request.getAreaSqm());
        room.setLandSizeSqm(request.getLandSizeSqm());
        room.setNumberOfFloors(request.getNumberOfFloors());
        room.setPropertyType(type);
        room.setListingScope(listingScopeFor(type));

        if (room.getPublicationStatus() == null) {
            room.setPublicationStatus(RoomPublicationStatus.DRAFT);
        }
        if (request.isPublish() && room.getPublicationStatus() != RoomPublicationStatus.PUBLISHED) {
            markPublished(room);
        }
    }

    private void markPublished(Room room) {
        LocalDateTime now = LocalDateTime.now();
        room.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        room.setApprovedAt(now);
        room.setApprovedBy(securityUtils.getCurrentUserId());
        room.setPublishedAt(now);
    }

    private static ListingScope listingScopeFor(PropertyType type) {
        if (PropertyTypeRules.isStructureLike(type)) {
            return ListingScope.ENTIRE_PROPERTY;
        }
        return ListingScope.INDIVIDUAL_ROOM;
    }

    private void replaceRoomAmenities(Long roomId, List<Long> amenityIds) {
        if (amenityIds == null) {
            return;
        }
        roomAmenityRepository.deleteByIdRoomId(roomId);
        roomAmenityRepository.flush();
        for (Long amenityId : amenityIds.stream().distinct().toList()) {
            Amenity amenity = amenityRepository.findById(amenityId)
                    .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + amenityId));
            roomAmenityRepository.save(RoomAmenity.builder()
                    .id(new RoomAmenityId(roomId, amenityId))
                    .amenity(amenity)
                    .build());
        }
    }

    private void replaceRoomUtilities(Long roomId, Long houseId, ResolvedOwner owner,
            List<AdminPropertyUtilityRequest> utilities) {
        if (utilities == null) {
            return;
        }
        roomUtilityRepository.deleteByRoomId(roomId);
        roomUtilityRepository.flush();
        for (AdminPropertyUtilityRequest utility : utilities) {
            if (utility.getUtilityTypeId() == null) {
                continue;
            }
            utilityTypeRepository.findById(utility.getUtilityTypeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Utility type not found with id: " + utility.getUtilityTypeId()));
            User assignmentOwner = owner.user != null ? owner.user : securityUtils.getCurrentUser();
            roomUtilityRepository.save(RoomUtility.builder()
                    .owner(assignmentOwner)
                    .roomId(roomId)
                    .utilityTypeId(utility.getUtilityTypeId())
                    .status(UtilityTypeStatus.ACTIVE)
                    .isIncluded(Boolean.TRUE.equals(utility.getIsIncluded()))
                    .build());
            if (utility.getRate() != null && houseId != null) {
                saveRate(houseId, utility.getUtilityTypeId(), utility.getRate(), assignmentOwner);
            }
        }
    }

    private void saveRate(Long houseId, Long utilityTypeId, BigDecimal rate, User owner) {
        utilityRateRepository.deleteByHouseIdAndUtilityTypeId(houseId, utilityTypeId);
        utilityRateRepository.flush();
        utilityRateRepository.save(UtilityRate.builder()
                .owner(owner)
                .houseId(houseId)
                .utilityTypeId(utilityTypeId)
                .rate(rate)
                .effectiveFrom(LocalDate.now())
                .status(UtilityRateStatus.ACTIVE)
                .build());
    }

    private void replaceNearbyLocations(House house, List<CreateNearbyLocationRequest> locations) {
        if (locations == null) {
            return;
        }
        nearbyLocationRepository.deleteByHouseId(house.getId());
        nearbyLocationRepository.flush();
        for (CreateNearbyLocationRequest location : locations) {
            nearbyLocationRepository.save(NearbyLocation.builder()
                    .house(house)
                    .name(location.name())
                    .category(location.category())
                    .distanceKm(location.distanceKm())
                    .travelMinutes(location.travelMinutes())
                    .build());
        }
    }

    private AdminPropertyResponse toResponse(Room room) {
        House house = room.getHouseId() != null
                ? houseRepository.findById(room.getHouseId()).orElse(null)
                : null;

        AdminPropertyResponse response = new AdminPropertyResponse();
        response.setRoomId(room.getId());
        response.setHouseId(room.getHouseId());
        if (house != null) {
            response.setPropertyName(house.getName());
            response.setAddress(house.getAddress());
            response.setPostalCode(house.getPostalCode());
            response.setCityProvince(house.getCityProvince());
        }
        response.setPropertyType(room.getPropertyType());
        response.setListingScope(room.getListingScope());
        response.setPublicationStatus(room.getPublicationStatus());
        response.setRoomNumber(room.getRoomNumber());
        response.setRoomName(room.getName());
        response.setFloorNumber(room.getFloorNumber());
        response.setCapacity(room.getCapacity());
        response.setRoomTypeId(room.getRoomTypeId());
        response.setRoomTypeName(room.getRoomTypeId() != null
                ? roomTypeRepository.findById(room.getRoomTypeId()).map(RoomType::getName).orElse(null)
                : null);
        response.setMonthlyRent(room.getMonthlyRent());
        response.setSecurityDeposit(room.getSecurityDeposit());
        response.setAdvancePayment(room.getAdvancePayment());
        response.setAvailableFrom(room.getAvailableFrom());
        response.setStatus(room.getStatus());
        response.setFurnishing(room.getFurnishing());
        response.setDescription(room.getDescription());
        response.setBedrooms(room.getBedrooms());
        response.setBathrooms(room.getBathrooms());
        response.setAreaSqm(room.getAreaSqm());
        response.setLandSizeSqm(room.getLandSizeSqm());
        response.setNumberOfFloors(room.getNumberOfFloors());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());
        response.setPublishedAt(room.getPublishedAt());
        response.setPhotoUrls(room.getHouseId() != null
                ? houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(room.getHouseId())
                        .stream().map(HouseImage::getSecureUrl).toList()
                : List.of());

        resolveOwnerIntoResponse(response, room, house);
        fillAmenities(response, room.getId());
        fillUtilities(response, room.getId(), house != null ? house.getId() : null);
        fillNearbyLocations(response, room.getHouseId());
        return response;
    }

    private void resolveOwnerIntoResponse(AdminPropertyResponse response, Room room, House house) {
        if (room.getOwnerId() != null) {
            userRepository.findById(room.getOwnerId()).ifPresent(user -> {
                response.setOwnerUserId(user.getId());
                response.setAccountOwner(true);
                response.setOwnerName(fullName(user));
                response.setOwnerEmail(user.getEmail());
                response.setOwnerPhone(user.getPhone());
            });
        } else if (room.getOwnerRecordId() != null) {
            propertyOwnerRepository.findById(room.getOwnerRecordId()).ifPresent(record -> {
                response.setPropertyOwnerId(record.getId());
                response.setOwnerName(record.getName());
                response.setOwnerEmail(record.getEmail());
                response.setOwnerPhone(record.getPhone());
            });
        }
        // Fall back to the house-level owner when the room row has no owner link.
        if (response.getOwnerName() == null && house != null) {
            if (house.getOwner() != null) {
                response.setOwnerUserId(house.getOwner().getId());
                response.setAccountOwner(true);
                response.setOwnerName(fullName(house.getOwner()));
                response.setOwnerEmail(house.getOwner().getEmail());
                response.setOwnerPhone(house.getOwner().getPhone());
            } else if (house.getOwnerRecordId() != null) {
                propertyOwnerRepository.findById(house.getOwnerRecordId()).ifPresent(record -> {
                    response.setPropertyOwnerId(record.getId());
                    response.setOwnerName(record.getName());
                    response.setOwnerEmail(record.getEmail());
                    response.setOwnerPhone(record.getPhone());
                });
            }
        }
    }

    private void fillAmenities(AdminPropertyResponse response, Long roomId) {
        List<RoomAmenity> amenities = roomAmenityRepository.findByIdRoomId(roomId);
        response.setAmenityIds(amenities.stream()
                .map(ra -> ra.getId().getAmenityId())
                .toList());
        response.setAmenityNames(amenities.stream()
                .map(ra -> ra.getAmenity().getName())
                .toList());
    }

    private void fillUtilities(AdminPropertyResponse response, Long roomId, Long houseId) {
        List<RoomUtility> assignments = roomUtilityRepository.findByRoomId(roomId);
        if (assignments.isEmpty()) {
            response.setUtilities(List.of());
            return;
        }
        List<Long> utilityTypeIds = assignments.stream()
                .map(RoomUtility::getUtilityTypeId)
                .distinct()
                .toList();
        Map<Long, BigDecimal> rates = currentRates(houseId, utilityTypeIds);
        List<AdminPropertyUtilityResponse> utilities = new ArrayList<>();
        for (RoomUtility assignment : assignments) {
            UtilityType type = utilityTypeRepository.findById(assignment.getUtilityTypeId()).orElse(null);
            if (type == null) {
                continue;
            }
            utilities.add(new AdminPropertyUtilityResponse(
                    type.getId(), type.getName(), type.getUnit(),
                    assignment.getIsIncluded(), rates.get(type.getId())));
        }
        response.setUtilities(utilities);
    }

    private void fillNearbyLocations(AdminPropertyResponse response, Long houseId) {
        if (houseId == null) {
            response.setNearbyLocations(List.of());
            return;
        }
        response.setNearbyLocations(nearbyLocationRepository
                .findByHouseIdOrderByDistanceKmAscIdAsc(houseId)
                .stream()
                .map(location -> new NearbyLocationResponse(
                        location.getId(),
                        location.getHouse().getId(),
                        location.getName(),
                        location.getCategory(),
                        location.getDistanceKm(),
                        location.getTravelMinutes()))
                .toList());
    }

    private Map<Long, BigDecimal> currentRates(Long houseId, List<Long> utilityTypeIds) {
        if (houseId == null || utilityTypeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> byType = new HashMap<>();
        for (UtilityRate rate : utilityRateRepository.findCurrentEffectiveRates(
                List.of(houseId), utilityTypeIds, LocalDate.now(), UtilityRateStatus.ACTIVE)) {
            byType.putIfAbsent(rate.getUtilityTypeId(), rate.getRate());
        }
        return byType;
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private static String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? (user.getUsername() == null ? "" : user.getUsername()) : name;
    }

    private static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s_-]+", "-");
        return slug.isEmpty() ? "property" : slug;
    }

    private static String randomToken(int length) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return token.substring(0, Math.min(length, token.length()));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Internal holder for the resolved owner of a post. */
    private static final class ResolvedOwner {

        private final User user;
        private final PropertyOwner record;

        private ResolvedOwner(User user, PropertyOwner record) {
            this.user = user;
            this.record = record;
        }
    }
}