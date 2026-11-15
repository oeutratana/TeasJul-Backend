package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.util.Map;

import com.example.spring_boot_project_api.dto.response.PayWayCheckTransactionResponse;
import com.example.spring_boot_project_api.dto.response.PayWayQrResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface PayWayService {

    PayWayQrResponse generateQr(String transactionId, BigDecimal amount,
            String currency, String callbackUrl, int lifetimeMinutes);

    /**
     * Verifies the PayWay callback signature from the X-Payway-Hmac-SHA512 header.
     * Sorts all JSON body keys alphabetically, concatenates values, computes HMAC-SHA512.
     */
    boolean verifyCallbackSignature(Map<String, Object> callbackBody, HttpServletRequest request);

    /**
     * Calls PayWay Check Transaction API to verify payment status.
     */
    PayWayCheckTransactionResponse checkTransaction(String transactionId);
}
