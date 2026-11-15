package com.example.spring_boot_project_api.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.ListingImageResponse;
import com.example.spring_boot_project_api.exception.ImageNotFoundException;
import com.example.spring_boot_project_api.mapper.ListingImageMapper;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.ListingImage;
import com.example.spring_boot_project_api.repository.ListingImageRepository;
import com.example.spring_boot_project_api.service.CloudinaryService;
import com.example.spring_boot_project_api.service.ListingAccessService;
import com.example.spring_boot_project_api.service.ListingImageService;
import com.example.spring_boot_project_api.util.ImageValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingImageServiceImpl implements ListingImageService {

    private static final String FOLDER_PREFIX = "teasjul/listings/";

    private final ListingImageRepository listingImageRepository;
    private final ListingAccessService listingAccessService;
    private final CloudinaryService cloudinaryService;
    private final ImageValidationUtil imageValidationUtil;
    private final ListingImageMapper listingImageMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ListingImageResponse> getImages(Long listingId) {
        listingAccessService.requireListingView(listingId);
        return toResponses(listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId));
    }

    @Override
    @Transactional
    public List<ListingImageResponse> upload(Long listingId, MultipartFile[] files) {
        Listing listing = listingAccessService.requireListingManage(listingId);

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        List<ListingImage> existing = listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId);
        boolean noImagesYet = existing.isEmpty();
        int baseOrder = existing.size();

        List<ListingImage> saved = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            imageValidationUtil.validate(file);
            CloudinaryService.UploadResult result = cloudinaryService.uploadImage(
                    readBytes(file), FOLDER_PREFIX + listingId);
            saved.add(ListingImage.builder()
                    .listing(listing)
                    .publicId(result.publicId())
                    .secureUrl(result.secureUrl())
                    .isPrimary(noImagesYet && i == 0)
                    .sortOrder(baseOrder + i)
                    .build());
        }

        listingImageRepository.saveAll(saved);
        return toResponses(listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId));
    }

    @Override
    @Transactional
    public List<ListingImageResponse> setPrimary(Long listingId, Long imageId) {
        listingAccessService.requireListingManage(listingId);

        ListingImage target = requireOwnedImage(listingId, imageId);
        List<ListingImage> images = listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId);
        for (ListingImage image : images) {
            image.setIsPrimary(Objects.equals(image.getId(), target.getId()));
        }
        listingImageRepository.saveAll(images);
        return toResponses(images);
    }

    @Override
    @Transactional
    public List<ListingImageResponse> updateOrder(Long listingId, Long imageId, Integer sortOrder) {
        listingAccessService.requireListingManage(listingId);

        ListingImage target = requireOwnedImage(listingId, imageId);
        List<ListingImage> images = listingImageRepository
                .findByListingIdOrderBySortOrderAscIdAsc(listingId);
        if (sortOrder == null || sortOrder < 0 || sortOrder >= images.size()) {
            throw new IllegalArgumentException("Invalid sort order: " + sortOrder);
        }

        List<ListingImage> reordered = new ArrayList<>(images);
        reordered.remove(target);
        reordered.add(sortOrder, target);
        for (int i = 0; i < reordered.size(); i++) {
            reordered.get(i).setSortOrder(i);
        }
        listingImageRepository.saveAll(images);
        return toResponses(reordered);
    }

    @Override
    @Transactional
    public void delete(Long listingId, Long imageId) {
        listingAccessService.requireListingManage(listingId);

        ListingImage image = requireOwnedImage(listingId, imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        try {
            cloudinaryService.deleteImage(image.getPublicId());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", image.getPublicId(), e.getMessage());
        }
        listingImageRepository.delete(image);

        if (wasPrimary) {
            listingImageRepository.findFirstByListingIdAndIsPrimaryTrue(listingId)
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        listingImageRepository.save(next);
                    });
        }
    }

    private ListingImage requireOwnedImage(Long listingId, Long imageId) {
        return listingImageRepository.findByIdAndListingId(imageId, listingId)
                .orElseThrow(() -> ImageNotFoundException.forId(imageId));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded image file", e);
        }
    }

    private List<ListingImageResponse> toResponses(List<ListingImage> images) {
        return images.stream().map(listingImageMapper::toResponse).toList();
    }
}