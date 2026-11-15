package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerDashboardResponse(
        OwnerKpis kpis,
        List<ManagerDashboardResponse.RevenuePoint> revenueTrend,
        List<ManagerDashboardResponse.RoomOccupancyPoint> roomOccupancy,
        List<ManagerDashboardResponse.StatusCount> paymentStatus,
        List<ManagerDashboardResponse.StatusCount> maintenanceStatus,
        List<HouseRevenuePoint> revenueByHouse,
        List<TopPerformingHouse> topHouses,
        List<RecentPayment> recentPayments,
        List<RecentActivity> recentActivities
) {

    public record OwnerKpis(
            long totalHouses,
            long totalRooms,
            long totalActiveTenants,
            double occupancyRate,
            BigDecimal totalRevenue,
            BigDecimal overdueAmount,
            BigDecimal paidDeposit
    ) {
    }

    public record HouseRevenuePoint(
            String houseName,
            BigDecimal revenue
    ) {
    }

    public record TopPerformingHouse(
            String houseName,
            long totalRooms,
            double occupancyRate,
            BigDecimal revenue,
            BigDecimal overdueAmount
    ) {
    }

    public record RecentPayment(
            Long id,
            String paymentNo,
            BigDecimal amount,
            String status,
            LocalDateTime paidAt,
            String tenantName,
            String houseName,
            String roomNumber
    ) {
    }

    public record RecentActivity(
            String type,
            String description,
            String houseName,
            LocalDateTime createdAt
    ) {
    }
}
