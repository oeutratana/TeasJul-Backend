package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.OwnerDashboardResponse;
import com.example.spring_boot_project_api.service.OwnerDashboardService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
@Tag(name = "Owner Dashboard", description = "Owner-specific dashboard KPIs, charts, top houses and recent activities")
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get owner dashboard",
            description = "Returns KPIs, charts, top performing houses, recent payments and activities "
                    + "scoped to the authenticated owner's houses.")
    @GetMapping
    public OwnerDashboardResponse getDashboard(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        securityUtils.requirePermission(PermissionConstants.DASHBOARD_VIEW);
        requireOwner();
        return ownerDashboardService.getDashboard(houseId, fromDate, toDate);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
