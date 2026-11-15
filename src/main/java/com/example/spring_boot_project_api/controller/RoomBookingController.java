package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RoomBookingRequest;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;
import com.example.spring_boot_project_api.service.RoomBookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/room-bookings")
@RequiredArgsConstructor
public class RoomBookingController {
    private final RoomBookingService roomBookingService;

    @PostMapping
    public ResponseEntity<RoomBookingResponse> create(@Valid @RequestBody RoomBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomBookingService.createBooking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomBookingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomBookingService.getBookingById(id));
    }

    @GetMapping
    public ResponseEntity<List<RoomBookingResponse>> list(@RequestParam(required = false) Long roomId) {
        return ResponseEntity.ok(roomBookingService.getBookings(roomId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RoomBookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(roomBookingService.cancelBooking(id));
    }
}
