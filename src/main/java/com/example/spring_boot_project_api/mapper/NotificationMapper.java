package com.example.spring_boot_project_api.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.model.Notification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() {
    };

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                parseData(notification.getData()),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }

    private static Map<String, Object> parseData(String data) {
        if (data == null || data.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(data, DATA_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}