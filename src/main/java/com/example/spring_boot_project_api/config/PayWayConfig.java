package com.example.spring_boot_project_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PayWayConfig {

    @Value("${payway.merchant-id:}")
    private String merchantId;

    @Value("${payway.api-key:}")
    private String apiKey;

    @Value("${payway.base-url:https://checkout-sandbox.payway.com.kh}")
    private String baseUrl;

    @Value("${payway.callback-url:}")
    private String callbackUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public boolean isConfigured() {
        return merchantId != null && !merchantId.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }
}
