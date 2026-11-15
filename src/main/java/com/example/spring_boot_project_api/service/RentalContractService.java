package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.ContractSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface RentalContractService {
    RentalContractResponse createContract(RentalContractRequest request);

    RentalContractResponse getContractById(Long id);

    PageResponse<RentalContractResponse> getContractsByOwner(Long ownerId, String search, RentalContractStatus status, Pageable pageable);

    List<RentalContractResponse> getContractsByRoom(Long roomId);

    PageResponse<RentalContractResponse> getContractsByStatus(RentalContractStatus status, Pageable pageable);

    ContractSummaryResponse getContractsSummary();

    RentalContractResponse updateContract(Long id, RentalContractRequest request);

    RentalContractResponse updateStatus(Long id, RentalContractStatus status);

    /**
     * Marks every ACTIVE contract whose end date has passed as EXPIRED and keeps
     * the affected room status consistent with the existing business rules.
     *
     * @return number of contracts transitioned to EXPIRED
     */
    int expireExpiredContracts();

    /**
     * Applies a successful renewal to the parent contract: advances the end date
     * to {@code newEndDate} and recomputes the status using the inclusive end-date
     * rule (a new end date of today or later keeps the contract ACTIVE; only a past
     * new end date becomes EXPIRED). The room status is kept consistent using the
     * existing {@code syncRoomStatus} business rule. Access control is the caller's
     * responsibility and is preserved unchanged.
     */
    void applyRenewal(Long contractId, LocalDate newEndDate);

    void deleteContract(Long id);
}
