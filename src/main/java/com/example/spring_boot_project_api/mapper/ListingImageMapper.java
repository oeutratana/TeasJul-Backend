package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ListingImageResponse;
import com.example.spring_boot_project_api.model.ListingImage;

@Component
public class ListingImageMapper {

    public ListingImageResponse toResponse(ListingImage image) {
        return new ListingImageResponse(
                image.getId(),
                image.getSecureUrl(),
                image.getIsPrimary(),
                image.getSortOrder());
    }
}