package com.example.spring_boot_project_api.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.service.ManagerReportService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owner/reports")
@RequiredArgsConstructor
@Tag(name = "Owner Reports", description = "Monthly reports for property owners")
public class OwnerReportController {

    private final ManagerReportService managerReportService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get owner monthly report",
            description = "Returns a monthly report scoped to the authenticated owner's houses. "
                    + "Reuses the report service with owner-level authorization.")
    @GetMapping("/monthly")
    public ManagerMonthlyReportResponse getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long houseId) {
        securityUtils.requirePermission(PermissionConstants.REPORT_VIEW);
        requireOwner();
        return managerReportService.getMonthlyReport(year, month, houseId);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
