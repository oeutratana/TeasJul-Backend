package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.Optional;
import com.example.spring_boot_project_api.dto.request.RoomBookingRequest;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;
import com.example.spring_boot_project_api.model.RoomBooking;

public interface RoomBookingService {
    RoomBookingResponse createBooking(RoomBookingRequest request);
    RoomBookingResponse getBookingById(Long id);
    List<RoomBookingResponse> getBookings(Long roomId);
    RoomBookingResponse cancelBooking(Long id);
    Optional<RoomBooking> findCurrentBookingForRoom(Long roomId);
    void completeBookingForContract(Long bookingId, Long roomId);
}
