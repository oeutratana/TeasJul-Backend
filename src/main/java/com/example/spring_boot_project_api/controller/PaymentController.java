package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreatePaymentRequest;
import com.example.spring_boot_project_api.dto.request.RecordManualPaymentRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PaymentListResponse;
import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.dto.response.PaymentStatusResponse;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "KHQR payment creation and management")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Operation(summary = "Create a KHQR payment",
            description = "Creates a pending payment for an invoice and generates a KHQR QR code "
                    + "via PayWay Sandbox. The payment amount is determined server-side from the invoice. "
                    + "The authenticated user must have access to the invoice's house.")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request));
    }

    @Operation(summary = "Record a manual (offline/cash) payment",
            description = "Creates an immediately-PAID payment record for an invoice (cash, bank "
                    + "transfer, etc.). The amount defaults to the full outstanding balance when omitted. "
                    + "Owner/manager only.")
    @PostMapping("/manual")
    public ResponseEntity<PaymentResponse> recordManualPayment(
            @Valid @RequestBody RecordManualPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordManualPayment(request));
    }

    @Operation(summary = "PayWay payment callback",
            description = "Receives payment status notifications from ABA PayWay. "
                    + "Signature is verified from the 'hash' field in the JSON body "
                    + "(fallback: X-PAYWAY-HMAC-SHA512 header). "
                    + "Payment success is always confirmed server-to-server via the Check Transaction API.")
    @PostMapping("/callback")
    public ResponseEntity<Void> handlePayWayCallback(
            @RequestBody(required = false) Map<String, Object> callback,
            HttpServletRequest httpRequest) {
        log.info("Received ABA PayWay callback");
        Map<String, Object> payload = new HashMap<>();
        if (callback != null) {
            payload.putAll(callback);
        }
        httpRequest.getParameterMap().forEach((key, values) ->
                payload.putIfAbsent(key, values.length > 0 ? values[0] : null));
        paymentService.processCallback(payload, httpRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get payment status",
            description = "Returns the authoritative status of a payment by ABA transaction id. "
                    + "Non-terminal payments are reconciled against ABA (Check Transaction) "
                    + "before the status is returned, so results are accurate even if the "
                    + "PayWay callback was delayed or lost.")
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(transactionId));
    }

    @Operation(summary = "List payments",
            description = "Returns payments across the houses the current owner/manager can access, "
                    + "optionally filtered by house, status, and created-date range.")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentListResponse>> getPayments(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPayments(houseId, status, fromDate, toDate, pageable));
    }
}
