package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record HouseManualSetupPreviewRequest(
        @NotEmpty(message = "At least one entry is required")
        List<HouseManualSetupPreviewRowRequest> rows
) {
    public record HouseManualSetupPreviewRowRequest(
            @NotNull(message = "Room number is required")
            String roomNumber,

            @NotNull(message = "Tenant first name is required")
            String tenantFirstName,

            String tenantLastName,

            String tenantPhone,

            String tenantEmail,

            @NotNull(message = "Move-in date is required")
            LocalDate moveInDate,

            @NotNull(message = "Monthly rent is required")
            @PositiveOrZero(message = "Monthly rent cannot be negative")
            BigDecimal monthlyRent,

            @PositiveOrZero(message = "Security deposit cannot be negative")
            BigDecimal securityDeposit,

            Integer dueDay,

            RentalContractStatus contractStatus,

            String notes
    ) {
    }
}
