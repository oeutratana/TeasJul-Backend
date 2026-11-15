package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreatePaymentRequest;
import com.example.spring_boot_project_api.dto.request.RecordManualPaymentRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PaymentListResponse;
import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.dto.response.PaymentStatusResponse;
import com.example.spring_boot_project_api.enums.PaymentStatus;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    /**
     * Records a manual (offline/cash) payment for an invoice.
     * Creates an immediately-PAID payment record linked to the given payment
     * method and updates the invoice status. Owner/manager only.
     */
    PaymentResponse recordManualPayment(RecordManualPaymentRequest request);

    /**
     * Processes a PayWay callback notification.
     * Verifies signature from the callback body (or X-Payway-Hmac-SHA512 header),
     * calls Check Transaction API, verifies amount/currency/txId,
     * updates payment and invoice status.
     */
    void processCallback(Map<String, Object> callback, HttpServletRequest request);

    /**
     * Returns the authoritative payment status for a transaction.
     * Non-terminal payments are reconciled against ABA (Check Transaction)
     * before the status is returned.
     */
    PaymentStatusResponse getPaymentStatus(String transactionId);

    /**
     * Returns payments for the houses the current owner/manager can access,
     * optionally filtered by house, status, and created-date range.
     */
    PageResponse<PaymentListResponse> getPayments(Long houseId, PaymentStatus status,
            LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
