package com.example.spring_boot_project_api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;
import com.example.spring_boot_project_api.dto.response.ListingImageResponse;
import com.example.spring_boot_project_api.dto.response.ListingResponse;
import com.example.spring_boot_project_api.dto.response.ListingRoomResponse;
import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;

/**
 * Converts {@link Listing} entities into {@link ListingResponse} DTOs.
 *
 * <p>House and room-type display names are resolved per listing. Use the
 * {@code (listing, houseName, roomTypeName, ...)} overload in batch flows to
 * avoid N+1 lookups.</p>
 */
@Component
public class ListingMapper {

    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;

    public ListingMapper(HouseRepository houseRepository, RoomTypeRepository roomTypeRepository) {
        this.houseRepository = houseRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public ListingResponse toResponse(Listing listing,
            List<ListingRoomResponse> rooms, List<ListingImageResponse> images,
            List<ListingAmenityResponse> amenities, List<ListingUtilityResponse> utilities) {
        return toResponse(listing,
                resolveHouseName(listing.getHouseId()),
                resolveRoomTypeName(listing.getRoomTypeId()),
                rooms, images, amenities, utilities);
    }

    public ListingResponse toResponse(Listing listing, String houseName, String roomTypeName,
            List<ListingRoomResponse> rooms, List<ListingImageResponse> images,
            List<ListingAmenityResponse> amenities, List<ListingUtilityResponse> utilities) {
        return new ListingResponse(
                listing.getId(),
                listing.getOwnerId(),
                listing.getHouseId(),
                houseName,
                listing.getRoomTypeId(),
                roomTypeName,
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
                listing.getBlueprintPublicId(),
                listing.getBlueprintSecureUrl(),
                listing.getPublicationStatus(),
                listing.getRejectionReason(),
                listing.getSubmittedAt(),
                listing.getApprovedAt(),
                listing.getPublishedAt(),
                listing.getRejectedAt(),
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                rooms,
                images,
                amenities,
                utilities);
    }

    private String resolveHouseName(Long houseId) {
        return houseId == null ? null
                : houseRepository.findById(houseId).map(House::getName).orElse(null);
    }

    private String resolveRoomTypeName(Long roomTypeId) {
        return roomTypeId == null ? null
                : roomTypeRepository.findById(roomTypeId).map(RoomType::getName).orElse(null);
    }
}