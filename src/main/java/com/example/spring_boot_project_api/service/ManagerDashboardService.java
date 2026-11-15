package com.example.spring_boot_project_api.service;

import java.time.LocalDate;

import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

public interface ManagerDashboardService {

    /**
     * Returns aggregated dashboard data for the currently authenticated owner or
     * manager, scoped to their accessible houses.
     *
     * @param houseId       optional single-house filter (must be accessible)
     * @param fromDate      inclusive start of the date range
     * @param toDate        inclusive end of the date range
     * @param roomStatus    optional room status filter applied to room metrics
     * @param paymentStatus optional invoice status filter for payment metrics
     */
    ManagerDashboardResponse getDashboard(Long houseId, LocalDate fromDate, LocalDate toDate,
            RoomStatus roomStatus, InvoiceStatus paymentStatus);
}
