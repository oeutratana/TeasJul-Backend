package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;
import com.example.spring_boot_project_api.model.RoomBooking;

@Component
public class RoomBookingMapper {
    public RoomBookingResponse toResponse(RoomBooking booking) {
        return new RoomBookingResponse(booking.getId(), booking.getRoomId(), booking.getCustomerName(), booking.getCustomerPhone(), booking.getCustomerEmail(), booking.getBookingDate(), booking.getExpectedMoveInDate(), booking.getStatus(), booking.getNote(), booking.getCreatedAt(), booking.getUpdatedAt());
    }
}
