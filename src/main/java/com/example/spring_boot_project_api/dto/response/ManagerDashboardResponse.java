package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate read model returned by {@code GET /api/dashboard} for owners and
 * managers. Only data within the authenticated user's accessible houses is
 * included; scoping is always resolved server-side.
 */
public record ManagerDashboardResponse(
        Kpis kpis,
        List<RevenuePoint> revenueTrend,
        List<RoomOccupancyPoint> roomOccupancy,
        List<StatusCount> paymentStatus,
        List<StatusCount> maintenanceStatus
) {

    public record Kpis(
            long totalRooms,
            long availableRooms,
            long occupiedRooms,
            long maintenanceRooms,
            BigDecimal revenue,
            double occupancyRate,
            long pendingInvoices,
            long overdueInvoices
    ) {
    }

    public record RevenuePoint(
            String month,
            BigDecimal amount
    ) {
    }

    public record RoomOccupancyPoint(
            String label,
            long occupied,
            long available,
            long maintenance
    ) {
    }

    public record StatusCount(
            String status,
            long count
    ) {
    }
}
