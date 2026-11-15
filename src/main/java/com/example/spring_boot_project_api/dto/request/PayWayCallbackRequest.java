package com.example.spring_boot_project_api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayWayCallbackRequest(
        @JsonProperty("tran_id") String tranId,
        @JsonProperty("apv") String approvedPaymentVoucher,
        @JsonProperty("status") String status,
        @JsonProperty("return_params") String returnParams,
        @JsonProperty("merchant_ref") String merchantRef
) {
    public boolean isSuccess() {
        return "0".equals(status);
    }
}
