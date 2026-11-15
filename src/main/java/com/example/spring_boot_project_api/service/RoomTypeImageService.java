package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.RoomTypeImageResponse;

public interface RoomTypeImageService {

    List<RoomTypeImageResponse> getImages(Long roomTypeId);

    List<RoomTypeImageResponse> upload(Long roomTypeId, MultipartFile[] files);

    List<RoomTypeImageResponse> setPrimary(Long roomTypeId, Long imageId);

    List<RoomTypeImageResponse> updateOrder(Long roomTypeId, Long imageId, Integer sortOrder);

    void delete(Long roomTypeId, Long imageId);
}