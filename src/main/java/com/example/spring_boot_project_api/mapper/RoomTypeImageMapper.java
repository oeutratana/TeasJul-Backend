package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.RoomTypeImageResponse;
import com.example.spring_boot_project_api.model.RoomTypeImage;

@Component
public class RoomTypeImageMapper {

    public RoomTypeImageResponse toResponse(RoomTypeImage image) {
        return new RoomTypeImageResponse(
                image.getId(),
                image.getSecureUrl(),
                image.getIsPrimary(),
                image.getSortOrder());
    }
}