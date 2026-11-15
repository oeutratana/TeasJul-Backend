package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateThemeRequest(

        @NotBlank(message = "Theme mode is required")
        @Pattern(regexp = "LIGHT|DARK|SYSTEM", message = "Theme mode must be LIGHT, DARK, or SYSTEM")
        String themeMode,

        @NotBlank(message = "Primary color is required")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Primary color must be a valid hex color like #2563EB")
        String primaryColor,

        @NotBlank(message = "Secondary color is required")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Secondary color must be a valid hex color like #0F172A")
        String secondaryColor
) {
}