package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.service.ManagerDashboardService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated dashboard KPIs and charts for owners and managers")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get manager/owner dashboard",
            description = "Returns KPIs and chart data scoped to the authenticated user's accessible "
                    + "houses. Owners see their own houses; managers see only houses assigned through "
                    + "user_house_access. All filters are applied server-side.")
    @GetMapping
    public ManagerDashboardResponse getDashboard(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) RoomStatus roomStatus,
            @RequestParam(required = false) InvoiceStatus paymentStatus) {
        securityUtils.requirePermission(PermissionConstants.DASHBOARD_VIEW);
        requireOwnerOrManager();
        return managerDashboardService.getDashboard(houseId, fromDate, toDate, roomStatus, paymentStatus);
    }

    private void requireOwnerOrManager() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}