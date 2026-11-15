package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.response.PaymentMethodResponse;
import com.example.spring_boot_project_api.service.PaymentMethodService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Payment methods available to the owner/manager")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @Operation(summary = "List payment methods",
            description = "Returns the payment methods configured for the current owner "
                    + "(and, for managers, for the owners whose houses they can access).")
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> listPaymentMethods() {
        return ResponseEntity.ok(paymentMethodService.listPaymentMethods());
    }

    @Operation(summary = "Create a payment method",
            description = "Adds a new payment method (e.g. Cash, Bakong transfer) owned by the current owner.")
    @PostMapping
    public ResponseEntity<PaymentMethodResponse> createPaymentMethod(
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentMethodService.createPaymentMethod(request));
    }

    @Operation(summary = "Update a payment method",
            description = "Updates the name, description, or status of an existing payment method (owner-only).")
    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> updatePaymentMethod(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(id, request));
    }
}