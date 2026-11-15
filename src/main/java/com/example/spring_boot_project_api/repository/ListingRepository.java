package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.model.Listing;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByHouseIdAndRoomTypeId(Long houseId, Long roomTypeId);

    Optional<Listing> findByIdAndOwnerId(Long id, Long ownerId);

    List<Listing> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    List<Listing> findByHouseIdInOrderByUpdatedAtDesc(Collection<Long> houseIds);

    Optional<Listing> findByIdAndPublicationStatus(Long id, RoomPublicationStatus publicationStatus);

    List<Listing> findByPublicationStatus(RoomPublicationStatus publicationStatus);

    List<Listing> findByHouseIdInAndPublicationStatus(Collection<Long> houseIds,
            RoomPublicationStatus publicationStatus);

    boolean existsByHouseIdAndRoomTypeId(Long houseId, Long roomTypeId);
}