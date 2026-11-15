package com.example.spring_boot_project_api.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.RoomTypeImageResponse;
import com.example.spring_boot_project_api.exception.ImageNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomTypeImageMapper;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomTypeImage;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.service.CloudinaryService;
import com.example.spring_boot_project_api.service.RoomTypeImageService;
import com.example.spring_boot_project_api.service.RoomTypeService;
import com.example.spring_boot_project_api.util.ImageValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTypeImageServiceImpl implements RoomTypeImageService {

    private static final String FOLDER_PREFIX = "teasjul/room-types/";

    private final RoomTypeImageRepository roomTypeImageRepository;
    private final RoomTypeService roomTypeService;
    private final CloudinaryService cloudinaryService;
    private final ImageValidationUtil imageValidationUtil;
    private final RoomTypeImageMapper roomTypeImageMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeImageResponse> getImages(Long roomTypeId) {
        roomTypeService.requireAccessibleRoomType(roomTypeId);
        return toResponses(roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId));
    }

    @Override
    @Transactional
    public List<RoomTypeImageResponse> upload(Long roomTypeId, MultipartFile[] files) {
        RoomType roomType = roomTypeService.requireAccessibleRoomType(roomTypeId);
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        List<RoomTypeImage> existing = roomTypeImageRepository
                .findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId);
        boolean noImagesYet = existing.isEmpty();
        int baseOrder = existing.size();

        List<RoomTypeImage> saved = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            imageValidationUtil.validate(file);
            CloudinaryService.UploadResult result = cloudinaryService.uploadImage(readBytes(file),
                    FOLDER_PREFIX + roomTypeId);
            saved.add(RoomTypeImage.builder()
                    .roomType(roomType)
                    .publicId(result.publicId())
                    .secureUrl(result.secureUrl())
                    .isPrimary(noImagesYet && i == 0)
                    .sortOrder(baseOrder + i)
                    .build());
        }

        roomTypeImageRepository.saveAll(saved);
        return toResponses(roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId));
    }

    @Override
    @Transactional
    public List<RoomTypeImageResponse> setPrimary(Long roomTypeId, Long imageId) {
        roomTypeService.requireAccessibleRoomType(roomTypeId);
        RoomTypeImage target = requireOwnedImage(roomTypeId, imageId);
        List<RoomTypeImage> images = roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId);
        for (RoomTypeImage image : images) {
            image.setIsPrimary(Objects.equals(image.getId(), target.getId()));
        }
        roomTypeImageRepository.saveAll(images);
        return toResponses(images);
    }

    @Override
    @Transactional
    public List<RoomTypeImageResponse> updateOrder(Long roomTypeId, Long imageId, Integer sortOrder) {
        roomTypeService.requireAccessibleRoomType(roomTypeId);
        RoomTypeImage target = requireOwnedImage(roomTypeId, imageId);
        List<RoomTypeImage> images = roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId);
        if (sortOrder == null || sortOrder < 0 || sortOrder >= images.size()) {
            throw new IllegalArgumentException("Invalid sort order: " + sortOrder);
        }

        List<RoomTypeImage> reordered = new ArrayList<>(images);
        reordered.remove(target);
        reordered.add(sortOrder, target);
        for (int i = 0; i < reordered.size(); i++) {
            reordered.get(i).setSortOrder(i);
        }
        roomTypeImageRepository.saveAll(images);
        return toResponses(reordered);
    }

    @Override
    @Transactional
    public void delete(Long roomTypeId, Long imageId) {
        roomTypeService.requireAccessibleRoomType(roomTypeId);
        RoomTypeImage image = requireOwnedImage(roomTypeId, imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        try {
            cloudinaryService.deleteImage(image.getPublicId());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", image.getPublicId(), e.getMessage());
        }
        roomTypeImageRepository.delete(image);

        if (wasPrimary) {
            roomTypeImageRepository.findFirstByRoomTypeIdOrderBySortOrderAscIdAsc(roomTypeId)
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        roomTypeImageRepository.save(next);
                    });
        }
    }

    private RoomTypeImage requireOwnedImage(Long roomTypeId, Long imageId) {
        return roomTypeImageRepository.findByIdAndRoomTypeId(imageId, roomTypeId)
                .orElseThrow(() -> ImageNotFoundException.forId(imageId));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded image file", e);
        }
    }

    private List<RoomTypeImageResponse> toResponses(List<RoomTypeImage> images) {
        return images.stream().map(roomTypeImageMapper::toResponse).toList();
    }
}