package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;

/**
 * Aggregated monthly reporting for house managers. All statistics are computed
 * with database-level aggregation and scoped to the authenticated user's
 * accessible houses (server-side).
 */
public interface ManagerReportService {

    /**
     * Builds the monthly management report for the given period.
     *
     * @param year    the report year (e.g. 2026)
     * @param month   the report month (1-12)
     * @param houseId optional single-house filter; when null every accessible
     *                house is included
     */
    ManagerMonthlyReportResponse getMonthlyReport(int year, int month, Long houseId);
}