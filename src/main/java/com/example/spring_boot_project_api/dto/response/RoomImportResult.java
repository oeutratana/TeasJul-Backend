package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record RoomImportResult(
        int roomsCreated,
        List<String> messages
) {
    public static RoomImportResult of(int roomsCreated, List<String> messages) {
        return new RoomImportResult(roomsCreated, messages);
    }
}