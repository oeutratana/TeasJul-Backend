package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record HouseImportResult(
        int roomsCreated,
        int tenantsCreated,
        int tenantsReused,
        int contractsCreated,
        int contractTenantsCreated,
        List<String> messages
) {
    public static HouseImportResult of(int roomsCreated, int tenantsCreated, int tenantsReused,
            int contractsCreated, int contractTenantsCreated, List<String> messages) {
        return new HouseImportResult(roomsCreated, tenantsCreated, tenantsReused, contractsCreated,
                contractTenantsCreated, messages);
    }
}