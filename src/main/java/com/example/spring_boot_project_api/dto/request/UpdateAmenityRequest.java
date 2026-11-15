package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.AmenityStatus;

import jakarta.validation.constraints.Size;

public record UpdateAmenityRequest(

    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    String description,

    @Size(max = 100, message = "Icon path/name must not exceed 100 characters")
    String icon,

    AmenityStatus status

) {}
