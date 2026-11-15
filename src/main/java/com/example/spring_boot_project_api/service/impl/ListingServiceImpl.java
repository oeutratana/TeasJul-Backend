package com.example.spring_boot_project_api.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.ListingRequest;
import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;
import com.example.spring_boot_project_api.dto.response.ListingBlueprintResponse;
import com.example.spring_boot_project_api.dto.response.ListingImageResponse;
import com.example.spring_boot_project_api.dto.response.ListingResponse;
import com.example.spring_boot_project_api.dto.response.ListingRoomResponse;
import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.ListingNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.ListingAmenityMapper;
import com.example.spring_boot_project_api.mapper.ListingImageMapper;
import com.example.spring_boot_project_api.mapper.ListingMapper;
import com.example.spring_boot_project_api.mapper.ListingUtilityMapper;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.ListingImage;
import com.example.spring_boot_project_api.model.ListingUtility;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.ListingAmenityRepository;
import com.example.spring_boot_project_api.repository.ListingImageRepository;
import com.example.spring_boot_project_api.repository.ListingRepository;
import com.example.spring_boot_project_api.repository.ListingUtilityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.CloudinaryService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.ListingAccessService;
import com.example.spring_boot_project_api.service.ListingService;
import com.example.spring_boot_project_api.util.ImageValidationUtil;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";
    private static final String BLUEPRINT_FOLDER_PREFIX = "teasjul/listings/";

    private final ListingRepository listingRepository;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ListingImageRepository listingImageRepository;
    private final ListingAmenityRepository listingAmenityRepository;
    private final ListingUtilityRepository listingUtilityRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final ListingAccessService listingAccessService;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final CloudinaryService cloudinaryService;
    private final ImageValidationUtil imageValidationUtil;
    private final ListingMapper listingMapper;
    private final ListingImageMapper listingImageMapper;
    private final ListingAmenityMapper listingAmenityMapper;
    private final ListingUtilityMapper listingUtilityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Listing> listings;
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            listings = listingRepository.findAll();
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            listings = listingRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUserId);
        } else if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> houseIds = houseAccessService.getAccessibleHouseIds();
            listings = houseIds.isEmpty() ? List.of()
                    : listingRepository.findByHouseIdInOrderByUpdatedAtDesc(houseIds);
        } else {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
        return listings.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListing(Long listingId) {
        return toResponse(listingAccessService.requireListingView(listingId));
    }

    @Override
    @Transactional
    public ListingResponse getOrCreate(Long houseId, Long roomTypeId) {
        securityUtils.requirePermission(PermissionConstants.LISTING_MANAGE);
        houseAccessService.requireOwnerHouseAccess(houseId);
        requireHouseAndTypeExists(houseId, roomTypeId);

        Listing listing = listingRepository.findByHouseIdAndRoomTypeId(houseId, roomTypeId)
                .orElseGet(() -> listingRepository.save(Listing.builder()
                        .ownerId(houseAccessServiceOwnerId(houseId))
                        .houseId(houseId)
                        .roomTypeId(roomTypeId)
                        .listingScope(com.example.spring_boot_project_api.enums.ListingScope.MULTIPLE_ROOMS)
                        .publicationStatus(RoomPublicationStatus.DRAFT)
                        .build()));
        if (!Objects.equals(listing.getOwnerId(), securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
        return toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingRoomResponse> getEligibleRooms(Long listingId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        List<Room> unattached = roomRepository
                .findByHouseIdAndRoomTypeIdAndStatusAndListingIdIsNull(
                        listing.getHouseId(), listing.getRoomTypeId(), RoomStatus.AVAILABLE);
        List<Room> attached = roomRepository
                .findByListingIdAndStatusOrderByRoomNumberAsc(listingId, RoomStatus.AVAILABLE);
        List<Room> merged = new ArrayList<>(attached);
        List<Long> attachedIds = attached.stream().map(Room::getId).toList();
        for (Room room : unattached) {
            if (!attachedIds.contains(room.getId())) {
                merged.add(room);
            }
        }
        return merged.stream().map(this::toRoomResponse).toList();
    }

    @Override
    @Transactional
    public ListingResponse create(ListingRequest request) {
        securityUtils.requirePermission(PermissionConstants.LISTING_MANAGE);
        houseAccessService.requireOwnerHouseAccess(request.houseId());
        requireHouseAndTypeExists(request.houseId(), request.roomTypeId());

        if (listingRepository.existsByHouseIdAndRoomTypeId(
                request.houseId(), request.roomTypeId())) {
            throw new DuplicateResourceException(
                    "A listing already exists for house " + request.houseId()
                            + " and room type " + request.roomTypeId());
        }

        Listing listing = Listing.builder()
                .ownerId(securityUtils.getCurrentUserId())
                .houseId(request.houseId())
                .roomTypeId(request.roomTypeId())
                .listingScope(request.listingScope() != null ? request.listingScope()
                        : com.example.spring_boot_project_api.enums.ListingScope.MULTIPLE_ROOMS)
                .publicationStatus(RoomPublicationStatus.DRAFT)
                .build();
        applyRequest(listing, request);
        Listing saved = listingRepository.save(listing);
        if (request.roomIds() != null && !request.roomIds().isEmpty()) {
            attachRooms(saved, request.roomIds());
        }
        return toResponse(listingRepository.findById(saved.getId())
                .orElseThrow(() -> ListingNotFoundException.forId(saved.getId())));
    }

    @Override
    @Transactional
    public ListingResponse update(Long listingId, ListingRequest request) {
        Listing listing = listingAccessService.requireOwnerListingManage(listingId);
        applyRequest(listing, request);
        if (request.roomIds() != null) {
            attachRooms(listing, request.roomIds());
        }
        return toResponse(listingRepository.save(listing));
    }

    @Override
    @Transactional
    public void delete(Long listingId) {
        Listing listing = listingAccessService.requireOwnerListingManage(listingId);

        List<Room> rooms = roomRepository.findByListingId(listingId);
        for (Room room : rooms) {
            room.setListingId(null);
            if (room.getPublicationStatus() == RoomPublicationStatus.PUBLISHED) {
                room.setPublicationStatus(RoomPublicationStatus.DRAFT);
            }
        }
        roomRepository.saveAll(rooms);

        for (ListingImage image : listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId)) {
            deleteCloudinaryAsset(image.getPublicId());
        }
        deleteCloudinaryAsset(listing.getBlueprintPublicId());

        listingRepository.deleteById(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingBlueprintResponse getBlueprint(Long listingId) {
        Listing listing = listingAccessService.requireListingView(listingId);
        return new ListingBlueprintResponse(
                listing.getId(), listing.getBlueprintPublicId(), listing.getBlueprintSecureUrl());
    }

    @Override
    @Transactional
    public ListingBlueprintResponse uploadBlueprint(Long listingId, MultipartFile file) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Blueprint image is required");
        }
        imageValidationUtil.validate(file);

        deleteCloudinaryAsset(listing.getBlueprintPublicId());

        CloudinaryService.UploadResult result = cloudinaryService.uploadImage(
                readBytes(file), BLUEPRINT_FOLDER_PREFIX + listingId + "/blueprint");
        listing.setBlueprintPublicId(result.publicId());
        listing.setBlueprintSecureUrl(result.secureUrl());
        listingRepository.save(listing);
        return new ListingBlueprintResponse(
                listing.getId(), listing.getBlueprintPublicId(), listing.getBlueprintSecureUrl());
    }

    @Override
    @Transactional
    public void deleteBlueprint(Long listingId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        if (listing.getBlueprintPublicId() == null) {
            return;
        }
        deleteCloudinaryAsset(listing.getBlueprintPublicId());
        listing.setBlueprintPublicId(null);
        listing.setBlueprintSecureUrl(null);
        listingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse toResponse(Listing listing) {
        if (listing == null) {
            return null;
        }
        List<ListingRoomResponse> rooms = roomRepository
                .findByListingIdOrderByRoomNumberAsc(listing.getId())
                .stream().map(this::toRoomResponse).toList();
        List<ListingImageResponse> images = listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listing.getId())
                .stream().map(listingImageMapper::toResponse).toList();
        List<ListingAmenityResponse> amenities = listingAmenityRepository
                .findByIdListingId(listing.getId())
                .stream().map(listingAmenityMapper::toResponse).toList();
        List<ListingUtilityResponse> utilities = toUtilityResponses(
                listingUtilityRepository.findByListingId(listing.getId()));
        return listingMapper.toResponse(listing, rooms, images, amenities, utilities);
    }

    private void applyRequest(Listing listing, ListingRequest request) {
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPropertyType(request.propertyType());
        if (request.listingScope() != null) {
            listing.setListingScope(request.listingScope());
        }
        listing.setFurnishing(request.furnishing());
        listing.setAvailableFrom(request.availableFrom());
        listing.setAdvancePayment(request.advancePayment());
        listing.setBedrooms(request.bedrooms());
        listing.setBathrooms(request.bathrooms());
        listing.setAreaSqm(request.areaSqm());
        listing.setSecurityDeposit(request.securityDeposit());
    }

    private void attachRooms(Listing listing, List<Long> roomIds) {
        List<Long> uniqueIds = roomIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Room> current = roomRepository.findByListingId(listing.getId());
        Map<Long, Room> currentById = current.stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));
        Set<Long> targetIds = new LinkedHashSet<>(uniqueIds);

        List<Room> changed = new ArrayList<>();
        for (Room room : current) {
            if (!targetIds.contains(room.getId())) {
                resetToDraft(room);
                room.setListingId(null);
                changed.add(room);
            }
        }

        for (Long roomId : uniqueIds) {
            if (currentById.containsKey(roomId)) {
                continue;
            }
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> RoomNotFoundException.forId(roomId));
            if (!Objects.equals(room.getHouseId(), listing.getHouseId())) {
                throw new IllegalArgumentException(
                        "Room " + roomId + " does not belong to the listing house");
            }
            if (!Objects.equals(room.getRoomTypeId(), listing.getRoomTypeId())) {
                throw new IllegalArgumentException(
                        "Room " + roomId + " does not match the listing room type");
            }
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalArgumentException(
                        "Only AVAILABLE rooms can be attached to a listing");
            }
            if (room.getListingId() != null && !room.getListingId().equals(listing.getId())) {
                throw new IllegalArgumentException(
                        "Room " + roomId + " is already attached to another listing");
            }
            resetToDraft(room);
            room.setListingId(listing.getId());
            changed.add(room);
        }

        if (!changed.isEmpty()) {
            roomRepository.saveAll(changed);
        }
    }

    private void resetToDraft(Room room) {
        if (room.getPublicationStatus() == RoomPublicationStatus.PUBLISHED) {
            room.setPublicationStatus(RoomPublicationStatus.DRAFT);
        }
    }

    private void requireHouseAndTypeExists(Long houseId, Long roomTypeId) {
        if (houseId == null || !houseRepository.existsById(houseId)) {
            throw HouseNotFoundException.forId(houseId);
        }
        if (roomTypeId == null || !roomTypeRepository.existsById(roomTypeId)) {
            throw RoomTypeNotFoundException.forId(roomTypeId);
        }
    }

    private Long houseAccessServiceOwnerId(Long houseId) {
        return securityUtils.getCurrentUserId();
    }

    private List<ListingUtilityResponse> toUtilityResponses(List<ListingUtility> assignments) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<Long> typeIds = assignments.stream()
                .map(ListingUtility::getUtilityTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UtilityType> typesById = utilityTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(UtilityType::getId, Function.identity(), (a, b) -> a));
        return assignments.stream()
                .map(a -> listingUtilityMapper.toResponse(a, typesById.get(a.getUtilityTypeId())))
                .toList();
    }

    private ListingRoomResponse toRoomResponse(Room room) {
        return new ListingRoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getName(),
                room.getFloorNumber(),
                room.getMonthlyRent(),
                room.getSecurityDeposit(),
                room.getStatus(),
                room.getPublicationStatus(),
                room.getCreatedAt());
    }

    private void deleteCloudinaryAsset(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinaryService.deleteImage(publicId);
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded blueprint image", e);
        }
    }
}