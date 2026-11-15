package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.ListingImageResponse;

/**
 * Listing gallery management (mirrors {@link HouseImageService}).
 */
public interface ListingImageService {

    List<ListingImageResponse> getImages(Long listingId);

    List<ListingImageResponse> upload(Long listingId, MultipartFile[] files);

    List<ListingImageResponse> setPrimary(Long listingId, Long imageId);

    List<ListingImageResponse> updateOrder(Long listingId, Long imageId, Integer sortOrder);

    void delete(Long listingId, Long imageId);
}