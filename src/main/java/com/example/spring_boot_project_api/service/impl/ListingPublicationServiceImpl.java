package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.exception.ListingNotFoundException;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.ListingRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.ListingPublicationService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Default implementation of {@link ListingPublicationService}.
 *
 * <p>Access rules (resolved server-side, never trusted from the client):</p>
 * <ul>
 *   <li>Submitting a listing requires the current user to be the listing owner,
 *       or a manager acting on the listing's house.</li>
 *   <li>Approving, rejecting and unpublishing are SUPER_ADMIN operations only,
 *       guarded by an explicit role check (never the generic permission check,
 *       because OWNER is implicitly granted every permission).</li>
 * </ul>
 *
 * <p>Approval and unpublish cascade to the listing's attached rooms so the
 * public website always shows consistent groups.</p>
 */
@Service
@RequiredArgsConstructor
public class ListingPublicationServiceImpl implements ListingPublicationService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final ListingRepository listingRepository;
    private final RoomRepository roomRepository;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Listing submitForApproval(Long listingId) {
        Listing listing = getAccessibleListing(listingId);
        if (listing.getPublicationStatus() != RoomPublicationStatus.DRAFT
                && listing.getPublicationStatus() != RoomPublicationStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only DRAFT or REJECTED listings can be submitted for approval");
        }

        listing.setPublicationStatus(RoomPublicationStatus.PENDING_APPROVAL);
        listing.setSubmittedAt(LocalDateTime.now());
        listing.setSubmittedBy(securityUtils.getCurrentUserId());
        Listing submitted = listingRepository.save(listing);
        notificationService.notifyPropertySubmitted(submitted);
        return submitted;
    }

    @Override
    @Transactional
    public Listing approve(Long listingId) {
        requireSuperAdmin();
        Listing listing = getListing(listingId);
        if (listing.getPublicationStatus() != RoomPublicationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL listings can be approved");
        }

        listing.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        listing.setApprovedAt(LocalDateTime.now());
        listing.setApprovedBy(securityUtils.getCurrentUserId());
        listing.setPublishedAt(LocalDateTime.now());
        listingRepository.save(listing);

        publishRooms(listingId);
        return listing;
    }

    @Override
    @Transactional
    public Listing reject(Long listingId, String rejectionReason) {
        requireSuperAdmin();
        Listing listing = getListing(listingId);
        if (listing.getPublicationStatus() != RoomPublicationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL listings can be rejected");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (rejectionReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Rejection reason cannot exceed 1000 characters");
        }

        listing.setPublicationStatus(RoomPublicationStatus.REJECTED);
        listing.setRejectedAt(LocalDateTime.now());
        listing.setRejectedBy(securityUtils.getCurrentUserId());
        listing.setRejectionReason(rejectionReason.trim());
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public Listing unpublish(Long listingId) {
        requireSuperAdmin();
        Listing listing = getListing(listingId);
        if (listing.getPublicationStatus() != RoomPublicationStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED listings can be unpublished");
        }

        listing.setPublicationStatus(RoomPublicationStatus.UNPUBLISHED);
        listingRepository.save(listing);

        List<Room> rooms = roomRepository.findByListingId(listingId);
        for (Room room : rooms) {
            room.setPublicationStatus(RoomPublicationStatus.UNPUBLISHED);
        }
        roomRepository.saveAll(rooms);
        return listing;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomPublicationStatus getPublicationStatus(Long listingId) {
        return getAccessibleListing(listingId).getPublicationStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> getListingsByPublicationStatus(RoomPublicationStatus publicationStatus) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return listingsWithStatus(publicationStatus);
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return listingRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUserId).stream()
                    .filter(l -> publicationStatus == null
                            || l.getPublicationStatus() == publicationStatus)
                    .toList();
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> houseIds = houseAccessService.getAccessibleHouseIds();
            return houseIds.isEmpty() ? List.of()
                    : listingRepository.findByHouseIdInOrderByUpdatedAtDesc(houseIds).stream()
                            .filter(l -> publicationStatus == null
                                    || l.getPublicationStatus() == publicationStatus)
                            .toList();
        }
        throw new AccessDeniedException(ACCESS_DENIED_MSG);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> getListingsForReview(RoomPublicationStatus publicationStatus) {
        requireSuperAdmin();
        return listingsWithStatus(publicationStatus);
    }

    private List<Listing> listingsWithStatus(RoomPublicationStatus publicationStatus) {
        if (publicationStatus == null) {
            return listingRepository.findAll().stream()
                    .sorted((a, b) -> {
                        int bySubmitted = java.util.Comparator
                                .<java.time.LocalDateTime>nullsFirst(
                                        java.util.Comparator.<java.time.LocalDateTime>naturalOrder())
                                .compare(a.getSubmittedAt(), b.getSubmittedAt());
                        return bySubmitted != 0 ? bySubmitted : a.getId().compareTo(b.getId());
                    })
                    .toList();
        }
        return listingRepository.findByPublicationStatus(publicationStatus).stream()
                .sorted((a, b) -> {
                    int bySubmitted = java.util.Comparator
                            .<java.time.LocalDateTime>nullsFirst(
                                    java.util.Comparator.<java.time.LocalDateTime>naturalOrder())
                            .compare(a.getSubmittedAt(), b.getSubmittedAt());
                    return bySubmitted != 0 ? bySubmitted : a.getId().compareTo(b.getId());
                })
                .toList();
    }

    private void publishRooms(Long listingId) {
        List<Room> rooms = roomRepository.findByListingId(listingId);
        LocalDateTime now = LocalDateTime.now();
        for (Room room : rooms) {
            room.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
            room.setApprovedAt(now);
            room.setApprovedBy(securityUtils.getCurrentUserId());
            room.setPublishedAt(now);
        }
        roomRepository.saveAll(rooms);
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> ListingNotFoundException.forId(listingId));
    }

    /**
     * Owner or manager access to the listing. The current user must be the
     * listing owner, or a manager whose managed houses include the listing house.
     */
    private Listing getAccessibleListing(Long listingId) {
        Listing listing = getListing(listingId);
        Long currentUserId = securityUtils.getCurrentUserId();
        if (listing.getOwnerId().equals(currentUserId)
                || securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            if (listing.getOwnerId().equals(currentUserId)
                    || houseAccessService.canManageHouse(listing.getHouseId())) {
                return listing;
            }
        }
        throw new AccessDeniedException(ACCESS_DENIED_MSG);
    }
}