package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.RoomTypeStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomTypeRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be over 100 characters")
    private String name;
    
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be over 50 characters")
    private String code;

    private String description;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity = 1;

    @NotNull
    @DecimalMin(value = "0.00", message = "Default rent must be positive")
    private BigDecimal defaultRent = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "Default deposit must be positive")
    private BigDecimal defaultDeposit = BigDecimal.ZERO;

    private RoomTypeStatus status = RoomTypeStatus.ACTIVE;
}
