package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.response.PaymentMethodResponse;
import com.example.spring_boot_project_api.enums.PaymentMethodStatus;
import com.example.spring_boot_project_api.exception.InvalidPaymentOperationException;
import com.example.spring_boot_project_api.exception.PaymentMethodNotFoundException;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.repository.PaymentMethodRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.PaymentMethodService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listPaymentMethods() {
        List<Long> ownerIds = houseAccessService.getScopedOwnerIds();
        if (ownerIds.isEmpty()) {
            return List.of();
        }
        return paymentMethodRepository.findByOwnerIdInOrderByIdAsc(ownerIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentMethodResponse createPaymentMethod(CreatePaymentMethodRequest request) {
        requireOwner();

        String code = normalizeCode(request.code());
        if (paymentMethodRepository.existsByOwnerIdAndCode(securityUtils.getCurrentUserId(), code)) {
            throw new InvalidPaymentOperationException(
                    "A payment method with code " + code + " already exists");
        }

        PaymentMethod method = PaymentMethod.builder()
                .owner(securityUtils.getCurrentUser())
                .name(request.name().trim())
                .code(code)
                .description(normalizeBlank(request.description()))
                .status(PaymentMethodStatus.ACTIVE)
                .build();

        return toResponse(paymentMethodRepository.save(method));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long id, UpdatePaymentMethodRequest request) {
        requireOwner();

        PaymentMethod method = paymentMethodRepository.findByIdAndOwnerId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> PaymentMethodNotFoundException.forId(id));

        method.setName(request.name().trim());
        method.setDescription(normalizeBlank(request.description()));
        method.setStatus(request.status());

        return toResponse(paymentMethodRepository.save(method));
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getName(),
                method.getCode(),
                method.getDescription(),
                method.getStatus());
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("Only the owner can manage payment methods");
        }
    }

    private String normalizeCode(String code) {
        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new InvalidPaymentOperationException("Payment method code is required");
        }
        return normalized;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}