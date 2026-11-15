package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.response.PaymentMethodResponse;

public interface PaymentMethodService {

    /** Returns the payment methods visible to the current owner/manager. */
    List<PaymentMethodResponse> listPaymentMethods();

    /** Creates a payment method owned by the current owner. */
    PaymentMethodResponse createPaymentMethod(CreatePaymentMethodRequest request);

    /** Updates the name/description/status of an existing payment method (owner-only). */
    PaymentMethodResponse updatePaymentMethod(Long id, UpdatePaymentMethodRequest request);
}