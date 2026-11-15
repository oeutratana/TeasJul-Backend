package com.example.spring_boot_project_api.dto.response;

public record HouseSummaryResponse(
        long totalRooms,
        long availableRooms,
        long occupiedRooms,
        long maintenanceRooms,
        long activeTenants,
        long activeContracts
) {
}
