package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ListingImage;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    List<ListingImage> findByListingIdOrderBySortOrderAscIdAsc(Long listingId);

    Optional<ListingImage> findByIdAndListingId(Long id, Long listingId);

    Optional<ListingImage> findFirstByListingIdAndIsPrimaryTrue(Long listingId);

    void deleteByListingId(Long listingId);
}