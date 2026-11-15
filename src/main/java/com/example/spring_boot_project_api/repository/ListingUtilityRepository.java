package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.model.ListingUtility;

@Repository
public interface ListingUtilityRepository extends JpaRepository<ListingUtility, Long> {

    List<ListingUtility> findByListingIdAndStatus(Long listingId, UtilityTypeStatus status);

    List<ListingUtility> findByListingIdInAndStatus(List<Long> listingIds, UtilityTypeStatus status);

    Optional<ListingUtility> findByListingIdAndUtilityTypeId(Long listingId, Long utilityTypeId);

    List<ListingUtility> findByListingId(Long listingId);
}