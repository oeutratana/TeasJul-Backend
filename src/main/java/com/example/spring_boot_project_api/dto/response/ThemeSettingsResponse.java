package com.example.spring_boot_project_api.dto.response;

public record ThemeSettingsResponse(
        Long ownerId,
        String themeMode,
        String primaryColor,
        String secondaryColor
) {
}