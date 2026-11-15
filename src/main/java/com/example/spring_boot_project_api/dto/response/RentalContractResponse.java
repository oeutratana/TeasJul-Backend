package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RentalContractResponse {
    private Long id;
    private Long ownerId;

    /**
     * Party A identity as snapshotted onto the contract at creation time. Null
     * fields mean the owner left that detail blank (or the contract predates the
     * snapshot) - they are not back-filled from current settings on read.
     */
    private String ownerFullName;
    private String ownerGender;
    private LocalDate ownerDateOfBirth;
    private String ownerNationality;
    private String ownerNationalId;
    private String ownerAddress;
    private String ownerPhone;
    private String ownerOccupation;

    private String contractNo;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer dueDay;
    private RentalContractStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
