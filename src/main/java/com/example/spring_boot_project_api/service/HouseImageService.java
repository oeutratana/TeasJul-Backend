package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.HouseImageResponse;

public interface HouseImageService {

    List<HouseImageResponse> getImages(Long houseId);

    List<HouseImageResponse> upload(Long houseId, MultipartFile[] files);

    List<HouseImageResponse> setPrimary(Long houseId, Long imageId);

    List<HouseImageResponse> updateOrder(Long houseId, Long imageId, Integer sortOrder);

    void delete(Long houseId, Long imageId);
}