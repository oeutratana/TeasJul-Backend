package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.UtilityTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.ListingUtilityMapper;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.ListingUtility;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ListingUtilityRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.ListingAccessService;
import com.example.spring_boot_project_api.service.ListingUtilityService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingUtilityServiceImpl implements ListingUtilityService {

    private final ListingUtilityRepository listingUtilityRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final ListingAccessService listingAccessService;
    private final SecurityUtils securityUtils;
    private final ListingUtilityMapper listingUtilityMapper;

    @Override
    @Transactional
    public ListingUtilityResponse assignUtility(Long listingId, Long utilityTypeId,
            Boolean isIncluded) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        UtilityType type = utilityTypeRepository.findById(utilityTypeId)
                .orElseThrow(() -> UtilityTypeNotFoundException.forId(utilityTypeId));

        if (listingUtilityRepository.findByListingIdAndUtilityTypeId(listing.getId(), utilityTypeId)
                .isPresent()) {
            throw new DuplicateResourceException(
                    "Utility type " + utilityTypeId + " is already assigned to listing " + listingId);
        }

        ListingUtility saved = listingUtilityRepository.save(listingUtilityMapper.toEntity(
                securityUtils.getCurrentUser(), listing.getId(), type, isIncluded, null));
        return listingUtilityMapper.toResponse(saved, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingUtilityResponse> getListingUtilities(Long listingId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        List<ListingUtility> assignments =
                listingUtilityRepository.findByListingId(listing.getId());
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

    @Override
    @Transactional
    public void removeUtility(Long listingId, Long utilityTypeId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        listingUtilityRepository
                .findByListingIdAndUtilityTypeId(listing.getId(), utilityTypeId)
                .ifPresent(listingUtilityRepository::delete);
    }
}