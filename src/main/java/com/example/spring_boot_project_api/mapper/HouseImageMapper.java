package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.HouseImageResponse;
import com.example.spring_boot_project_api.model.HouseImage;

@Component
public class HouseImageMapper {

    public HouseImageResponse toResponse(HouseImage image) {
        return new HouseImageResponse(
                image.getId(),
                image.getSecureUrl(),
                image.getIsPrimary(),
                image.getSortOrder());
    }
}