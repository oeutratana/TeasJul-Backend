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
@RequestMapping("/api/manager/reports")
@RequiredArgsConstructor
@Tag(name = "Manager Reports", description = "Monthly management reports with DB-aggregated statistics")
public class ManagerReportController {

    private final ManagerReportService managerReportService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get monthly management report",
            description = "Returns a single report for the given year/month with summary sections and "
                    + "chart datasets, scoped to the authenticated manager's accessible houses "
                    + "(or a single house when houseId is provided). All statistics are aggregated "
                    + "server-side.")
    @GetMapping("/monthly")
    public ManagerMonthlyReportResponse getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long houseId) {
        securityUtils.requirePermission(PermissionConstants.REPORT_VIEW);
        requireManager();
        return managerReportService.getMonthlyReport(year, month, houseId);
    }

    private void requireManager() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}