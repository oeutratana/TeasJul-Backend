package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.spring_boot_project_api.enums.RoomBookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomBookingResponse {
    private final Long id;
    private final Long roomId;
    private final String customerName;
    private final String customerPhone;
    private final String customerEmail;
    private final LocalDate bookingDate;
    private final LocalDate expectedMoveInDate;
    private final RoomBookingStatus status;
    private final String note;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
