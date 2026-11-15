package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.dto.response.PublicListingResponse;
import com.example.spring_boot_project_api.dto.response.PublicListingRoom;
import com.example.spring_boot_project_api.dto.response.PublicUtilityResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.ListingNotFoundException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.ListingImage;
import com.example.spring_boot_project_api.model.ListingUtility;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomTypeImage;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.ListingAmenityRepository;
import com.example.spring_boot_project_api.repository.ListingImageRepository;
import com.example.spring_boot_project_api.repository.ListingRepository;
import com.example.spring_boot_project_api.repository.ListingUtilityRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.ListingAccessService;
import com.example.spring_boot_project_api.service.PublicListingService;

import lombok.RequiredArgsConstructor;

/**
 * Aggregates a grouped listing into its public shape.
 *
 * <p>No authentication for {@link #getPublicListing} (PUBLISHED listings only);
 * {@link #getPreview} reuses the exact same aggregation for the owner/manager
 * builder so the "Preview Public Listing" page is pixel-identical to the public
 * page — but shows every attached AVAILABLE room even before approval.</p>
 */
@Service
@RequiredArgsConstructor
public class PublicListingServiceImpl implements PublicListingService {

    private static final int MAX_DETAIL_IMAGES = 8;

    private final ListingRepository listingRepository;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ListingImageRepository listingImageRepository;
    private final HouseImageRepository houseImageRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final ListingAmenityRepository listingAmenityRepository;
    private final ListingUtilityRepository listingUtilityRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityRateRepository utilityRateRepository;
    private final NearbyLocationRepository nearbyLocationRepository;
    private final RoomRepository roomRepository;
    private final ListingAccessService listingAccessService;

    @Override
    @Transactional(readOnly = true)
    public PublicListingResponse getPublicListing(Long listingId) {
        Listing listing = listingRepository
                .findByIdAndPublicationStatus(listingId, RoomPublicationStatus.PUBLISHED)
                .orElseThrow(() -> ListingNotFoundException.forId(listingId));
        return toPublicResponse(listing, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicListingResponse getPreview(Long listingId) {
        Listing listing = listingAccessService.requireListingView(listingId);
        return toPublicResponse(listing, true);
    }

    private PublicListingResponse toPublicResponse(Listing listing, boolean includeNonPublishedRooms) {
        House house = listing.getHouseId() == null ? null
                : houseRepository.findById(listing.getHouseId()).orElse(null);
        RoomType roomType = listing.getRoomTypeId() == null ? null
                : roomTypeRepository.findById(listing.getRoomTypeId()).orElse(null);

        List<String> listingImageUrls = listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listing.getId()).stream()
                .map(ListingImage::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        List<String> houseImageUrls = house == null ? List.of()
                : houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(house.getId()).stream()
                        .map(HouseImage::getSecureUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .toList();
        List<String> roomTypeImageUrls = roomType == null ? List.of()
                : roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomType.getId()).stream()
                        .map(RoomTypeImage::getSecureUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .toList();

        List<String> images = new ArrayList<>();
        images.addAll(listingImageUrls);
        images.addAll(roomTypeImageUrls);
        images.addAll(houseImageUrls);
        images = images.stream().distinct().limit(MAX_DETAIL_IMAGES).toList();

        List<String> amenities = listingAmenityRepository.findByIdListingId(listing.getId()).stream()
                .map(la -> la.getAmenity() == null ? null : la.getAmenity().getName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        List<PublicListingRoom> rooms = roomRepository
                .findByListingIdOrderByRoomNumberAsc(listing.getId()).stream()
                .filter(r -> includeNonPublishedRooms
                        || (r.getPublicationStatus() == RoomPublicationStatus.PUBLISHED))
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .map(r -> new PublicListingRoom(
                        r.getId(), r.getRoomNumber(), r.getName(), r.getFloorNumber(),
                        r.getCapacity(), r.getMonthlyRent(), r.getSecurityDeposit(), r.getStatus()))
                .toList();

        Long availableRoomCount = (long) rooms.size();
        BigDecimal fromRent = rooms.stream().map(PublicListingRoom::monthlyRent)
                .filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxRent = rooms.stream().map(PublicListingRoom::monthlyRent)
                .filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null);

        return new PublicListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPropertyType(),
                listing.getListingScope(),
                listing.getFurnishing(),
                listing.getAvailableFrom(),
                listing.getAdvancePayment(),
                listing.getBedrooms(),
                listing.getBathrooms(),
                listing.getAreaSqm(),
                listing.getSecurityDeposit(),
                listing.getBlueprintSecureUrl(),
                listing.getPublishedAt(),
                listing.getHouseId(),
                house == null ? null : house.getName(),
                house == null ? null : house.getDescription(),
                house == null ? null : house.getAddress(),
                house == null ? null : house.getPostalCode(),
                house == null ? null : house.getCityProvince(),
                house == null ? null : house.getLatitude(),
                house == null ? null : house.getLongitude(),
                house == null ? null : house.getGoogleMapsUrl(),
                houseImageUrls,
                listing.getRoomTypeId(),
                roomType == null ? null : roomType.getName(),
                roomType == null ? null : roomType.getDescription(),
                roomTypeImageUrls,
                images,
                availableRoomCount,
                fromRent,
                maxRent,
                rooms,
                amenities,
                publicUtilities(listing),
                house == null ? List.of() : toNearbyResponses(house.getId()));
    }

    /**
     * Resolves listing-level utilities with their current effective rates in
     * two queries (mirrors the public room utility derivation).
     */
    private List<PublicUtilityResponse> publicUtilities(Listing listing) {
        List<ListingUtility> assignments = listingUtilityRepository
                .findByListingIdAndStatus(listing.getId(), UtilityTypeStatus.ACTIVE);
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<Long> typeIds = assignments.stream()
                .map(ListingUtility::getUtilityTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UtilityType> typesById = utilityTypeRepository
                .findByIdInAndStatus(typeIds, UtilityTypeStatus.ACTIVE).stream()
                .collect(Collectors.toMap(UtilityType::getId, Function.identity(), (a, b) -> a));

        List<Long> billedTypeIds = assignments.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsIncluded()))
                .map(ListingUtility::getUtilityTypeId)
                .filter(typesById::containsKey)
                .distinct()
                .toList();
        Map<Long, BigDecimal> ratesByTypeId = currentRates(listing.getHouseId(), billedTypeIds);

        List<PublicUtilityResponse> result = new ArrayList<>();
        for (ListingUtility assignment : assignments) {
            UtilityType type = typesById.get(assignment.getUtilityTypeId());
            if (type == null) {
                continue;
            }
            boolean included = Boolean.TRUE.equals(assignment.getIsIncluded());
            BigDecimal rate = included ? null : ratesByTypeId.get(type.getId());
            result.add(new PublicUtilityResponse(type.getId(), type.getName(), type.getUnit(),
                    type.getBillingMethod(), rate, included));
        }
        return result;
    }

    private Map<Long, BigDecimal> currentRates(Long houseId, List<Long> utilityTypeIds) {
        if (houseId == null || utilityTypeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> rates = new HashMap<>();
        for (UtilityRate rate : utilityRateRepository.findCurrentEffectiveRates(
                List.of(houseId), utilityTypeIds, LocalDate.now(), UtilityRateStatus.ACTIVE)) {
            rates.putIfAbsent(rate.getUtilityTypeId(), rate.getRate());
        }
        return rates;
    }

    private List<NearbyLocationResponse> toNearbyResponses(Long houseId) {
        return nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(houseId).stream()
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
}