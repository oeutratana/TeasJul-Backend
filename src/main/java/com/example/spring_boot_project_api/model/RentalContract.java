package com.example.spring_boot_project_api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rental_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * Party A identity, snapshotted from the owner's contract settings when the
     * contract was created. Every field is nullable: contracts created before the
     * snapshot existed have none, and owners may leave optional fields blank.
     *
     * <p>These columns are never rewritten while the contract keeps the same owner,
     * so a contract keeps showing the details it was signed with even after the
     * owner updates their settings.</p>
     */
    @Column(name = "owner_full_name", length = 200)
    private String ownerFullName;

    @Column(name = "owner_gender", length = 20)
    private String ownerGender;

    @Column(name = "owner_date_of_birth")
    private LocalDate ownerDateOfBirth;

    @Column(name = "owner_nationality", length = 100)
    private String ownerNationality;

    @Column(name = "owner_national_id", length = 100)
    private String ownerNationalId;

    @Column(name = "owner_address", length = 255)
    private String ownerAddress;

    @Column(name = "owner_phone", length = 30)
    private String ownerPhone;

    @Column(name = "owner_occupation", length = 150)
    private String ownerOccupation;

    @Column(name = "contract_no", nullable = false, length = 50)
    private String contractNo;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /**
     * The reservation consumed by this contract, when the room was booked.
     * Direct rentals deliberately leave this null.
     */
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Builder.Default
    @Column(name = "security_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "due_day", nullable = false)
    private Integer dueDay = 5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RentalContractStatus status = RentalContractStatus.DRAFT;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
