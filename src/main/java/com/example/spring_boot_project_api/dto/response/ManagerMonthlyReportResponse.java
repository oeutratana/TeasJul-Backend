package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full read model returned by {@code GET /api/manager/reports/monthly}.
 * All numbers are aggregated in the database and scoped to the authenticated
 * manager's accessible houses (server-side). Charts arrays are included so the
 * frontend can render them without extra requests.
 */
public record ManagerMonthlyReportResponse(
        ReportMeta meta,
        RoomStats rooms,
        FinancialSummary financial,
        ContractSummary contracts,
        PaymentSummary payments,
        UtilitySummary utilities,
        MaintenanceSummary maintenance,
        List<MonthlyRevenuePoint> revenueTrend,
        List<StatusCount> paymentStatus,
        List<StatusCount> roomOccupancy,
        List<StatusCount> maintenanceStatus
) {

    public record ReportMeta(
            int year,
            int month,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Long houseId,
            String houseName,
            LocalDateTime generatedAt
    ) {
    }

    public record RoomStats(
            long totalRooms,
            long occupiedRooms,
            long availableRooms,
            long reservedRooms,
            long maintenanceRooms,
            double occupancyRate
    ) {
    }

    public record FinancialSummary(
            BigDecimal totalRent,
            BigDecimal totalPaid,
            BigDecimal totalOutstanding,
            BigDecimal totalDeposits,
            BigDecimal totalExpenses,
            BigDecimal netRevenue
    ) {
    }

    public record ContractSummary(
            long newContracts,
            long activeContracts,
            long expiredContracts,
            long terminatedContracts,
            long renewedContracts
    ) {
    }

    public record PaymentSummary(
            long paidPayments,
            long pendingPayments,
            long overduePayments,
            BigDecimal totalPaymentAmount
    ) {
    }

    public record UtilitySummary(
            BigDecimal totalWaterUsage,
            BigDecimal totalElectricityUsage,
            BigDecimal totalUtilityCharges,
            long paidUtilityBills,
            long outstandingUtilityBills
    ) {
    }

    public record MaintenanceSummary(
            long totalRequests,
            long openRequests,
            long inProgressRequests,
            long completedRequests,
            long cancelledRequests,
            BigDecimal totalCost
    ) {
    }

    public record MonthlyRevenuePoint(
            String month,
            BigDecimal amount
    ) {
    }

    public record StatusCount(
            String status,
            long count
    ) {
    }
}