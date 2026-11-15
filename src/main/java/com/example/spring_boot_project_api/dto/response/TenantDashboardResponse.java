package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

/**
 * Aggregate read model returned by {@code GET /api/tenant/dashboard}.
 *
 * <p>Only data owned by the currently authenticated tenant is included. When the
 * tenant has no active rental contract the room/contract sections are {@code null}
 * and the financial/utilities sections are zeroed/empty safe states.
 */
public record TenantDashboardResponse(
        TenantDashboardTenant tenant,
        TenantDashboardRoom room,
        TenantDashboardContract contract,
        TenantDashboardFinancial financial,
        TenantDashboardUtilities utilities
) {

    public record TenantDashboardTenant(
            Long id,
            String name
    ) {
    }

    public record TenantDashboardRoom(
            Long id,
            String houseName,
            String roomNumber,
            String roomName
    ) {
    }

    public record TenantDashboardContract(
            Long id,
            String contractNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            Integer dueDay,
            RentalContractStatus status
    ) {
    }

    public record TenantDashboardFinancial(
            BigDecimal currentDue,
            BigDecimal totalPaid,
            long unpaidInvoices,
            long overdueInvoices,
            long paidInvoices
    ) {
    }

    public record TenantDashboardUtilities(
            UtilityUsageResponse water,
            UtilityUsageResponse electricity
    ) {
    }
}