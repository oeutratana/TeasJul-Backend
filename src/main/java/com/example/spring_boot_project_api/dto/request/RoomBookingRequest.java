package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomBookingRequest {
    @NotNull(message = "Room ID is required") private Long roomId;
    @NotBlank(message = "Customer name is required") @Size(max = 100, message = "Customer name must not exceed 100 characters") private String customerName;
    @NotBlank(message = "Customer phone is required") @Size(max = 30, message = "Customer phone must not exceed 30 characters") private String customerPhone;
    @Email(message = "Customer email must be valid") @Size(max = 150, message = "Customer email must not exceed 150 characters") private String customerEmail;
    @NotNull(message = "Expected move-in date is required") @FutureOrPresent(message = "Expected move-in date cannot be in the past") private LocalDate expectedMoveInDate;
    @Size(max = 2000, message = "Note must not exceed 2000 characters") private String note;
}
