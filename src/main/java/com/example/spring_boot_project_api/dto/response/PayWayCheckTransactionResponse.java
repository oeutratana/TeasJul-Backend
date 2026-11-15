package com.example.spring_boot_project_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayWayCheckTransactionResponse(
        @JsonProperty("data") TransactionData data,
        @JsonProperty("status") TransactionStatus status
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionData(
            @JsonProperty("payment_status_code") Integer paymentStatusCode,
            @JsonProperty("payment_status") String paymentStatus,
            @JsonProperty("payment_amount") Double paymentAmount,
            @JsonProperty("payment_currency") String paymentCurrency,
            @JsonProperty("total_amount") Double totalAmount,
            @JsonProperty("original_amount") Double originalAmount,
            @JsonProperty("refund_amount") Double refundAmount,
            @JsonProperty("discount_amount") Double discountAmount,
            @JsonProperty("apv") String apv,
            @JsonProperty("transaction_date") String transactionDate
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionStatus(
            @JsonProperty("code") Object code,
            @JsonProperty("message") String message,
            @JsonProperty("tran_id") String tranId
    ) {}
}
