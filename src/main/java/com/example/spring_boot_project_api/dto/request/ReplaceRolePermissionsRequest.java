package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplaceRolePermissionsRequest(

        @NotNull(message = "Permission ids are required")
        List<@NotNull @Positive Long> permissionIds
) {
}
