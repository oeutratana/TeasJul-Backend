package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.PublicRoomQueryRequest;
import com.example.spring_boot_project_api.dto.response.AvailableRoomResponse;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PublicOwnerResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomTypeResponse;
import com.example.spring_boot_project_api.dto.response.PublicUtilityResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.model.PropertyOwner;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomTypeImage;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.repository.PropertyOwnerRepository;
import com.example.spring_boot_project_api.repository.PublicRoomGroupProjection;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.PublicRoomService;

import lombok.RequiredArgsConstructor;

/**
 * Default implementation of {@link PublicRoomService}.
 *
 * <p>No authentication or authorization is required. Only rooms whose
 * publication status is {@code PUBLISHED} are returned; internal business
 * data (owners, tenants, contracts, payments) is never exposed.</p>
 */
@Service
@RequiredArgsConstructor
public class PublicRoomServiceImpl implements PublicRoomService {

    private static final int MAX_DETAIL_IMAGES = 8;

    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HouseImageRepository houseImageRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final NearbyLocationRepository nearbyLocationRepository;
    private final RoomUtilityRepository roomUtilityRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityRateRepository utilityRateRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicRoomResponse> listRooms(PublicRoomQueryRequest query, Pageable pageable) {
        PublicRoomQueryRequest req = query != null ? query
                : new PublicRoomQueryRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        String trimmedSearch = (req.search() == null || req.search().isBlank()) ? null : req.search().trim();
        String trimmedCity = (req.cityProvince() == null || req.cityProvince().isBlank()) ? null : req.cityProvince().trim();
        String sortOrder = req.sort() == null ? "NEWEST" : req.sort().name();

        List<String> amenities = sanitizeList(req.amenities());
        List<String> utilities = sanitizeList(req.utilities());

        Page<PublicRoomGroupProjection> page = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED,
                RoomStatus.AVAILABLE,
                trimmedSearch,
                trimmedCity,
                req.propertyType(),
                req.roomTypeId(),
                req.listingScope(),
                req.furnishing(),
                req.minRent(),
                req.maxRent(),
                req.bedrooms(),
                req.bathrooms(),
                req.minAreaSqm(),
                req.maxAreaSqm(),
                req.capacity(),
                req.availableFrom(),
                !amenities.isEmpty(),
                amenities,
                !utilities.isEmpty(),
                utilities,
                sortOrder,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));

        Map<Long, Room> roomsById = roomRepository.findAllById(page.getContent().stream()
                .map(PublicRoomGroupProjection::getRepresentativeRoomId)
                .toList())
                .stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));
        List<Room> representativeRooms = page.getContent().stream()
                .map(group -> roomsById.get(group.getRepresentativeRoomId()))
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, House> houses = housesById(representativeRooms);
        Map<Long, RoomType> roomTypes = roomTypesById(representativeRooms);
        Map<Long, String> houseImages = primaryHouseImages(representativeRooms);
        Map<Long, String> roomTypeImages = primaryRoomTypeImages(representativeRooms);
        Map<Long, List<NearbyLocationResponse>> nearbyLocations = nearbyLocationsByHouseIds(
                representativeRooms);
        Map<Long, List<PublicUtilityResponse>> utilityResponses = publicUtilitiesByRoomIds(representativeRooms);

        List<PublicRoomResponse> content = page.getContent().stream()
                .map(group -> toListItem(roomsById.get(group.getRepresentativeRoomId()), houses,
                        roomTypes, houseImages, roomTypeImages, nearbyLocations, utilityResponses,
                        group.getAvailableRoomCount(), group.getFromRent()))
                .filter(java.util.Objects::nonNull)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private List<String> sanitizeList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicRoomTypeResponse> getPublicRoomTypes() {
        List<Long> ids = roomRepository.findDistinctPublicRoomTypeIds(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE);
        if (ids.isEmpty()) {
            return List.of();
        }
        return roomTypeRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(RoomType::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(roomType -> new PublicRoomTypeResponse(roomType.getId(), roomType.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicRoomResponse getRoom(Long id) {
        Room room = roomRepository.findByIdAndPublicationStatusAndStatus(id,
                        RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE)
                .orElseThrow(() -> RoomNotFoundException.forId(id));
        return toDetail(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicRoomResponse> getSimilarRooms(Long id) {
        Room room = roomRepository.findByIdAndPublicationStatusAndStatus(id,
                        RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE)
                .orElseThrow(() -> RoomNotFoundException.forId(id));

        if (room.getHouseId() == null || room.getRoomTypeId() == null) {
            return List.of();
        }

        List<PublicRoomGroupProjection> similarGroups = roomRepository.findSimilarRoomGroups(
                room.getHouseId(),
                room.getRoomTypeId(),
                room.getMonthlyRent() == null ? java.math.BigDecimal.ZERO : room.getMonthlyRent(),
                RoomPublicationStatus.PUBLISHED,
                RoomStatus.AVAILABLE,
                PageRequest.of(0, 4));

        Map<Long, Room> roomsById = roomRepository.findAllById(similarGroups.stream()
                .map(PublicRoomGroupProjection::getRepresentativeRoomId)
                .toList())
                .stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));
        List<Room> representativeRooms = similarGroups.stream()
                .map(group -> roomsById.get(group.getRepresentativeRoomId()))
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, House> houses = housesById(representativeRooms);
        Map<Long, RoomType> roomTypes = roomTypesById(representativeRooms);
        Map<Long, String> houseImages = primaryHouseImages(representativeRooms);
        Map<Long, String> roomTypeImages = primaryRoomTypeImages(representativeRooms);
        Map<Long, List<NearbyLocationResponse>> nearbyLocations = nearbyLocationsByHouseIds(
                representativeRooms);
        Map<Long, List<PublicUtilityResponse>> utilities = publicUtilitiesByRoomIds(representativeRooms);

        return similarGroups.stream()
                .map(group -> toListItem(roomsById.get(group.getRepresentativeRoomId()), houses,
                        roomTypes, houseImages, roomTypeImages, nearbyLocations, utilities,
                        group.getAvailableRoomCount(), group.getFromRent()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private PublicRoomResponse toListItem(Room room, Map<Long, House> houses,
            Map<Long, RoomType> roomTypes, Map<Long, String> houseImages,
            Map<Long, String> roomTypeImages, Map<Long, List<NearbyLocationResponse>> nearbyLocations,
            Map<Long, List<PublicUtilityResponse>> utilities, Long availableRoomCount,
            BigDecimal fromRent) {
        if (room == null) {
            return null;
        }
        House house = houses.get(room.getHouseId());
        RoomType roomType = roomTypes.get(room.getRoomTypeId());
        String houseImage = houseImages.get(room.getHouseId());
        String roomTypeImage = roomTypeImages.get(room.getRoomTypeId());
        return new PublicRoomResponse(
                room.getId(),
                room.getListingId(),
                room.getName(),
                room.getRoomNumber(),
                room.getFloorNumber(),
                room.getCapacity(),
                room.getMonthlyRent(),
                room.getSecurityDeposit(),
                room.getStatus(),
                room.getDescription(),
                room.getHouseId(),
                house == null ? null : house.getName(),
                house == null ? null : house.getAddress(),
                house == null ? null : house.getPostalCode(),
                houseImage,
                room.getRoomTypeId(),
                roomType == null ? null : roomType.getName(),
                roomTypeImage,
                nonNullImages(roomTypeImage, houseImage),
                List.of(),
                room.getPublishedAt(),
                room.getPropertyType(),
                room.getListingScope(),
                room.getFurnishing(),
                room.getAvailableFrom(),
                room.getAdvancePayment(),
                room.getBedrooms(),
                room.getBathrooms(),
                room.getAreaSqm(),
                room.getLandSizeSqm(),
                room.getNumberOfFloors(),
                house == null ? null : house.getCityProvince(),
                house == null ? null : house.getLatitude(),
                house == null ? null : house.getLongitude(),
                house == null ? null : house.getGoogleMapsUrl(),
                nearbyLocations.getOrDefault(room.getHouseId(), List.of()),
                utilities.getOrDefault(room.getId(), List.of()),
                availableRoomCount,
                fromRent,
                List.of(),
                null);
    }

    private PublicRoomResponse toDetail(Room room) {
        House house = room.getHouseId() == null ? null
                : houseRepository.findById(room.getHouseId()).orElse(null);
        RoomType roomType = room.getRoomTypeId() == null ? null
                : roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);

        String houseImage = house == null ? null : primaryImage(house.getId());
        String roomTypeImage = roomType == null ? null : roomTypePrimaryImage(roomType.getId());

        List<String> images = new ArrayList<>();
        if (roomType != null) {
            roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomType.getId())
                    .stream().map(RoomTypeImage::getSecureUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .forEach(images::add);
        }
        if (house != null) {
            houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(house.getId())
                    .stream().map(HouseImage::getSecureUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .forEach(images::add);
        }
        List<String> uniqueImages = images.stream()
                .distinct()
                .limit(MAX_DETAIL_IMAGES)
                .toList();

        List<String> amenities = roomAmenityRepository.findByIdRoomId(room.getId()).stream()
                .map(ra -> ra.getAmenity() == null ? null : ra.getAmenity().getName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        return new PublicRoomResponse(
                room.getId(),
                room.getListingId(),
                room.getName(),
                room.getRoomNumber(),
                room.getFloorNumber(),
                room.getCapacity(),
                room.getMonthlyRent(),
                room.getSecurityDeposit(),
                room.getStatus(),
                room.getDescription(),
                room.getHouseId(),
                house == null ? null : house.getName(),
                house == null ? null : house.getAddress(),
                house == null ? null : house.getPostalCode(),
                houseImage,
                room.getRoomTypeId(),
                roomType == null ? null : roomType.getName(),
                roomTypeImage,
                uniqueImages.isEmpty() ? nonNullImages(roomTypeImage, houseImage) : uniqueImages,
                amenities,
                room.getPublishedAt(),
                room.getPropertyType(),
                room.getListingScope(),
                room.getFurnishing(),
                room.getAvailableFrom(),
                room.getAdvancePayment(),
                room.getBedrooms(),
                room.getBathrooms(),
                room.getAreaSqm(),
                room.getLandSizeSqm(),
                room.getNumberOfFloors(),
                house == null ? null : house.getCityProvince(),
                house == null ? null : house.getLatitude(),
                house == null ? null : house.getLongitude(),
                house == null ? null : house.getGoogleMapsUrl(),
                house == null ? List.of() : toNearbyResponses(house.getId()),
                publicUtilitiesByRoomIds(List.of(room)).getOrDefault(room.getId(), List.of()),
                null,
                null,
                availableRooms(room),
                toPublicOwner(house));
    }

    private PublicOwnerResponse toPublicOwner(House house) {
        if (house == null) {
            return null;
        }
        if (house.getOwner() != null) {
            User owner = house.getOwner();
            String name = java.util.stream.Stream.of(owner.getFirstName(), owner.getLastName())
                    .filter(part -> part != null && !part.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
            return new PublicOwnerResponse(owner.getId(), name.isBlank() ? null : name,
                    owner.getPhone() == null || owner.getPhone().isBlank() ? null : owner.getPhone().trim(),
                    owner.getEmail() == null || owner.getEmail().isBlank() ? null : owner.getEmail().trim());
        }
        // Owner without a TeasJul login account, recorded through the admin
        // property-posting flow (houses.owner_record_id).
        if (house.getOwnerRecordId() != null) {
            PropertyOwner record = propertyOwnerRepository.findById(house.getOwnerRecordId())
                    .orElse(null);
            if (record != null) {
                return new PublicOwnerResponse(record.getId(),
                        record.getName() == null || record.getName().isBlank() ? null : record.getName().trim(),
                        record.getPhone() == null || record.getPhone().isBlank() ? null : record.getPhone().trim(),
                        record.getEmail() == null || record.getEmail().isBlank() ? null : record.getEmail().trim());
            }
        }
        return null;
    }

    private List<AvailableRoomResponse> availableRooms(Room representativeRoom) {
        if (representativeRoom.getHouseId() == null || representativeRoom.getRoomTypeId() == null) {
            return List.of();
        }

        return roomRepository
                .findByHouseIdAndRoomTypeIdAndPublicationStatusAndStatusOrderByFloorNumberAscRoomNumberAscIdAsc(
                        representativeRoom.getHouseId(), representativeRoom.getRoomTypeId(),
                        RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE)
                .stream()
                .map(room -> new AvailableRoomResponse(room.getId(), room.getRoomNumber(),
                        room.getFloorNumber(), room.getMonthlyRent(), room.getSecurityDeposit(),
                        room.getStatus()))
                .toList();
    }
    private Map<Long, List<PublicUtilityResponse>> publicUtilitiesByRoomIds(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        Map<Long, Room> roomsById = rooms.stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));
        List<Long> roomIds = new ArrayList<>(roomsById.keySet());
        List<RoomUtility> assignments = roomUtilityRepository.findByRoomIdInAndStatus(
                roomIds, UtilityTypeStatus.ACTIVE);
        if (assignments.isEmpty()) {
            return Map.of();
        }

        List<Long> utilityTypeIds = assignments.stream()
                .map(RoomUtility::getUtilityTypeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (utilityTypeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UtilityType> typesById = utilityTypeRepository
                .findByIdInAndStatus(utilityTypeIds, UtilityTypeStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(UtilityType::getId, Function.identity(), (a, b) -> a));

        List<Long> billedUtilityTypeIds = assignments.stream()
                .filter(assignment -> !Boolean.TRUE.equals(assignment.getIsIncluded()))
                .map(RoomUtility::getUtilityTypeId)
                .filter(typesById::containsKey)
                .distinct()
                .toList();
        Map<HouseUtilityKey, BigDecimal> ratesByHouseAndType = currentRates(
                roomsById, billedUtilityTypeIds);

        Map<Long, List<PublicUtilityResponse>> result = new LinkedHashMap<>();
        for (RoomUtility assignment : assignments) {
            Room room = roomsById.get(assignment.getRoomId());
            UtilityType type = typesById.get(assignment.getUtilityTypeId());
            if (room == null || type == null) {
                continue;
            }
            boolean included = Boolean.TRUE.equals(assignment.getIsIncluded());
            BigDecimal rate = included ? null
                    : ratesByHouseAndType.get(new HouseUtilityKey(room.getHouseId(), type.getId()));
            result.computeIfAbsent(room.getId(), ignored -> new ArrayList<>())
                    .add(new PublicUtilityResponse(type.getId(), type.getName(), type.getUnit(),
                            type.getBillingMethod(), rate, included));
        }
        return result;
    }

    private Map<HouseUtilityKey, BigDecimal> currentRates(Map<Long, Room> roomsById,
            List<Long> utilityTypeIds) {
        if (utilityTypeIds.isEmpty()) {
            return Map.of();
        }
        List<Long> houseIds = roomsById.values().stream()
                .map(Room::getHouseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (houseIds.isEmpty()) {
            return Map.of();
        }
        Map<HouseUtilityKey, BigDecimal> rates = new HashMap<>();
        for (UtilityRate rate : utilityRateRepository.findCurrentEffectiveRates(
                houseIds, utilityTypeIds, LocalDate.now(), UtilityRateStatus.ACTIVE)) {
            rates.putIfAbsent(new HouseUtilityKey(rate.getHouseId(), rate.getUtilityTypeId()),
                    rate.getRate());
        }
        return rates;
    }

    private record HouseUtilityKey(Long houseId, Long utilityTypeId) {
    }

    private Map<Long, House> housesById(List<Room> rooms) {
        List<Long> ids = rooms.stream().map(Room::getHouseId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        return houseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(House::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, RoomType> roomTypesById(List<Room> rooms) {
        List<Long> ids = rooms.stream().map(Room::getRoomTypeId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        return roomTypeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> primaryHouseImages(List<Room> rooms) {
        Map<Long, String> result = new LinkedHashMap<>();
        List<Long> houseIds = rooms.stream().map(Room::getHouseId).distinct().toList();
        for (HouseImage image : houseImageRepository.findPrimaryByHouseIds(houseIds)) {
            result.putIfAbsent(image.getHouse().getId(), image.getSecureUrl());
        }
        return result;
    }

    private Map<Long, List<NearbyLocationResponse>> nearbyLocationsByHouseIds(List<Room> rooms) {
        Map<Long, List<NearbyLocationResponse>> result = new LinkedHashMap<>();
        List<Long> houseIds = rooms.stream().map(Room::getHouseId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        for (NearbyLocation location : nearbyLocationRepository.findByHouseIdIn(houseIds)) {
            result.computeIfAbsent(location.getHouse().getId(),
                    k -> new ArrayList<>()).add(toNearbyResponse(location));
        }
        return result;
    }

    private List<NearbyLocationResponse> toNearbyResponses(Long houseId) {
        return nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(houseId)
                .stream()
                .map(this::toNearbyResponse)
                .toList();
    }

    private NearbyLocationResponse toNearbyResponse(NearbyLocation location) {
        return new NearbyLocationResponse(
                location.getId(),
                location.getHouse().getId(),
                location.getName(),
                location.getCategory(),
                location.getDistanceKm(),
                location.getTravelMinutes());
    }

    private Map<Long, String> primaryRoomTypeImages(List<Room> rooms) {
        Map<Long, String> result = new LinkedHashMap<>();
        List<Long> roomTypeIds = rooms.stream().map(Room::getRoomTypeId).distinct().toList();
        for (RoomTypeImage image : roomTypeImageRepository.findPrimaryByRoomTypeIds(roomTypeIds)) {
            result.putIfAbsent(image.getRoomType().getId(), image.getSecureUrl());
        }
        return result;
    }

    private String primaryImage(Long houseId) {
        return houseImageRepository.findFirstByHouseIdAndIsPrimaryTrue(houseId)
                .map(HouseImage::getSecureUrl)
                .or(() -> houseImageRepository
                        .findFirstByHouseIdOrderBySortOrderAscIdAsc(houseId)
                        .map(HouseImage::getSecureUrl))
                .orElse(null);
    }

    private String roomTypePrimaryImage(Long roomTypeId) {
        return roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId).stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .findFirst()
                .map(RoomTypeImage::getSecureUrl)
                .or(() -> roomTypeImageRepository
                        .findFirstByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId)
                        .map(RoomTypeImage::getSecureUrl))
                .orElse(null);
    }

    private List<String> nonNullImages(String... urls) {
        return java.util.Arrays.stream(urls)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
    }
}

