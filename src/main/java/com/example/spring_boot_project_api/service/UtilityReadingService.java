package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.enums.ReadingStatusFilter;

public interface UtilityReadingService {

    UtilityReadingResponse createReading(CreateUtilityReadingRequest request);

    PageResponse<UtilityReadingResponse> getReadings(Long meterId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    UtilityReadingResponse getReadingById(Long id);

    List<MonthlyReadingRowResponse> getMonthlySheet(Long houseId, Integer year, Integer month,
            Integer floor, String search, ReadingStatusFilter status);

    /**
     * Distinct floor numbers (sorted ascending) that currently have ACTIVE
     * meters within the user's accessible house scope. A null {@code houseId}
     * means all accessible houses.
     */
    List<Integer> getMonthlySheetFloors(Long houseId);

    BulkUtilityReadingResponse createReadingsBulk(MonthlyReadingBulkRequest request);
}
