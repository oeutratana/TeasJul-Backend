package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.InvoiceSummaryResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;

public interface InvoiceSummaryService {

    /**
     * Returns the invoice financial summary for the current user's accessible
     * houses (owner houses, or houses the manager has access to), narrowed by
     * the optional filters.
     *
     * @param houseId optional house filter; the current user must be authorized
     *                for the given house or an {@code AccessDeniedException} is thrown.
     * @param year    optional billing year filter (also drives the monthly chart year).
     * @param month   optional billing month filter (affects the headline stats,
     *                not the monthly chart series).
     * @param status  optional invoice status filter.
     */
    InvoiceSummaryResponse getSummary(Long houseId, Integer year, Integer month, InvoiceStatus status);
}