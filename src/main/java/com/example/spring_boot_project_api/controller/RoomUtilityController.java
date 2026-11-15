package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RoomUtilityRequest;
import com.example.spring_boot_project_api.dto.response.RoomUtilityResponse;
import com.example.spring_boot_project_api.service.RoomUtilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomId}/utilities")
@RequiredArgsConstructor
@Tag(name = "Room Utilities",
        description = "Manage which utility types are billed for a room")
public class RoomUtilityController {

    private final RoomUtilityService roomUtilityService;

    @Operation(summary = "List utilities assigned to a room")
    @GetMapping
    public ResponseEntity<List<RoomUtilityResponse>> getRoomUtilities(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(roomUtilityService.getRoomUtilities(roomId));
    }

    @Operation(summary = "Assign a utility type to a room")
    @PostMapping("/{utilityTypeId}")
    public ResponseEntity<RoomUtilityResponse> assignUtility(
            @PathVariable Long roomId,
            @PathVariable Long utilityTypeId,
            @RequestBody(required = false) RoomUtilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomUtilityService.assignUtility(roomId, utilityTypeId,
                        request == null ? false : request.isIncluded()));
    }

    @Operation(summary = "Remove a utility type from a room")
    @DeleteMapping("/{utilityTypeId}")
    public ResponseEntity<Void> removeUtility(
            @PathVariable Long roomId,
            @PathVariable Long utilityTypeId) {
        roomUtilityService.removeUtility(roomId, utilityTypeId);
        return ResponseEntity.noContent().build();
    }
}
