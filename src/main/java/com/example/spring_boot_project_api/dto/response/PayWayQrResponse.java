package com.example.spring_boot_project_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayWayQrResponse(
        @JsonProperty("qrString")
        String qrString,

        @JsonProperty("qrImage")
        String qrImage,

        @JsonProperty("abapay_deeplink")
        String abapayDeeplink,

        @JsonProperty("amount")
        Double amount,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("status")
        PayWayStatus status
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PayWayStatus(
            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("trace_id")
            String traceId
    ) {
    }
}
