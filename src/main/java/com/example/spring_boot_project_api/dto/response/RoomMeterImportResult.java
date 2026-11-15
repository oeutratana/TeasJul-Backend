package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record RoomMeterImportResult(
        int roomsCreated,
        int metersCreated,
        List<String> messages
) {
    public static RoomMeterImportResult of(int roomsCreated, int metersCreated, List<String> messages) {
        return new RoomMeterImportResult(roomsCreated, metersCreated, messages);
    }
}