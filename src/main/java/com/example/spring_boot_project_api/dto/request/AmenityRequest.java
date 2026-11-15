package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.AmenityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmenityRequest (

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    String description,

    @Size(max = 100, message = "Icon path/name must not exceed 100 characters")
    String icon,

    AmenityStatus status

){}
