package com.example.spring_boot_project_api.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.HouseImageResponse;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.ImageNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseImageMapper;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.service.CloudinaryService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.HouseImageService;
import com.example.spring_boot_project_api.util.ImageValidationUtil;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseImageServiceImpl implements HouseImageService {

    private static final String FOLDER_PREFIX = "teasjul/houses/";

    private final HouseImageRepository houseImageRepository;
    private final HouseRepository houseRepository;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final CloudinaryService cloudinaryService;
    private final ImageValidationUtil imageValidationUtil;
    private final HouseImageMapper houseImageMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HouseImageResponse> getImages(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        houseAccessService.requireHouseAccess(houseId);
        return toResponses(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(houseId));
    }

    @Override
    @Transactional
    public List<HouseImageResponse> upload(Long houseId, MultipartFile[] files) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);
        requireHouseExists(houseId);

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        List<HouseImage> existing = houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(houseId);
        boolean noImagesYet = existing.isEmpty();
        int baseOrder = existing.size();

        List<HouseImage> saved = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            imageValidationUtil.validate(file);
            CloudinaryService.UploadResult result = cloudinaryService.uploadImage(readBytes(file),
                    FOLDER_PREFIX + houseId);
            saved.add(HouseImage.builder()
                    .house(houseRepository.getReferenceById(houseId))
                    .publicId(result.publicId())
                    .secureUrl(result.secureUrl())
                    .isPrimary(noImagesYet && i == 0)
                    .sortOrder(baseOrder + i)
                    .build());
        }

        houseImageRepository.saveAll(saved);
        return toResponses(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(houseId));
    }

    @Override
    @Transactional
    public List<HouseImageResponse> setPrimary(Long houseId, Long imageId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);

        HouseImage target = requireOwnedImage(houseId, imageId);
        List<HouseImage> images = houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(houseId);
        for (HouseImage image : images) {
            image.setIsPrimary(Objects.equals(image.getId(), target.getId()));
        }
        houseImageRepository.saveAll(images);
        return toResponses(images);
    }

    @Override
    @Transactional
    public List<HouseImageResponse> updateOrder(Long houseId, Long imageId, Integer sortOrder) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);

        HouseImage target = requireOwnedImage(houseId, imageId);
        List<HouseImage> images = houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(houseId);
        if (sortOrder == null || sortOrder < 0 || sortOrder >= images.size()) {
            throw new IllegalArgumentException("Invalid sort order: " + sortOrder);
        }

        List<HouseImage> reordered = new ArrayList<>(images);
        reordered.remove(target);
        reordered.add(sortOrder, target);
        for (int i = 0; i < reordered.size(); i++) {
            reordered.get(i).setSortOrder(i);
        }
        houseImageRepository.saveAll(images);
        return toResponses(reordered);
    }

    @Override
    @Transactional
    public void delete(Long houseId, Long imageId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);

        HouseImage image = requireOwnedImage(houseId, imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        try {
            cloudinaryService.deleteImage(image.getPublicId());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", image.getPublicId(), e.getMessage());
        }
        houseImageRepository.delete(image);

        if (wasPrimary) {
            houseImageRepository.findFirstByHouseIdOrderBySortOrderAscIdAsc(houseId)
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        houseImageRepository.save(next);
                    });
        }
    }

    private HouseImage requireOwnedImage(Long houseId, Long imageId) {
        return houseImageRepository.findByIdAndHouseId(imageId, houseId)
                .orElseThrow(() -> ImageNotFoundException.forId(imageId));
    }

    private void requireHouseExists(Long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw HouseNotFoundException.forId(houseId);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded image file", e);
        }
    }

    private List<HouseImageResponse> toResponses(List<HouseImage> images) {
        return images.stream().map(houseImageMapper::toResponse).toList();
    }
}