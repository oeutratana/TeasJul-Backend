package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.model.UtilityReading;

@Component
public class UtilityReadingMapper {

    public UtilityReadingResponse toResponse(UtilityReading reading) {
        return new UtilityReadingResponse(
                reading.getId(),
                reading.getMeter().getId(),
                reading.getPreviousReading(),
                reading.getCurrentReading(),
                reading.getUsage(),
                reading.getReadingDate(),
                reading.getCreatedAt());
    }

    public UtilityReading toEntity(CreateUtilityReadingRequest request) {
        return UtilityReading.builder()
                .currentReading(request.currentReading())
                .readingDate(request.readingDate())
                .build();
    }
}
