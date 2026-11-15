package com.example.spring_boot_project_api.service;

import java.time.LocalDate;

import com.example.spring_boot_project_api.dto.response.OwnerDashboardResponse;

public interface OwnerDashboardService {

    OwnerDashboardResponse getDashboard(Long houseId, LocalDate fromDate, LocalDate toDate);
}
