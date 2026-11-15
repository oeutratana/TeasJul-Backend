package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ListingAmenity;
import com.example.spring_boot_project_api.model.ListingAmenityId;
@Repository
public interface ListingAmenityRepository extends JpaRepository<ListingAmenity, ListingAmenityId> {

    @EntityGraph(attributePaths = "amenity")
    List<ListingAmenity> findByIdListingId(Long listingId);

    boolean existsByIdListingIdAndIdAmenityId(Long listingId, Long amenityId);

    Optional<ListingAmenity> findByIdListingIdAndIdAmenityId(Long listingId, Long amenityId);

    void deleteByIdListingIdAndIdAmenityId(Long listingId, Long amenityId);

    long countByIdListingId(Long listingId);
}