package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Inline room type for an admin-posted ROOM listing. The backend generates the
 * unique code, so the frontend only has to provide a display name and defaults.
 */
@Data
public class AdminPropertyRoomTypeRequest {

    @NotBlank(message = "Room type name is required")
    @Size(max = 100, message = "Room type name must not exceed 100 characters")
    private String name;

    private String description;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity = 1;

    private BigDecimal defaultRent = BigDecimal.ZERO;

    private BigDecimal defaultDeposit = BigDecimal.ZERO;
}