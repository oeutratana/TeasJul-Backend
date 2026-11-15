package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.config.PayWayConfig;
import com.example.spring_boot_project_api.dto.response.PayWayCheckTransactionResponse;
import com.example.spring_boot_project_api.dto.response.PayWayQrResponse;
import com.example.spring_boot_project_api.exception.PayWayIntegrationException;
import com.example.spring_boot_project_api.service.impl.PayWayServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayWayServiceImplTest {

    @Mock private PayWayConfig payWayConfig;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private PayWayServiceImpl payWayService;

    @BeforeEach
    void setUp() {
        when(payWayConfig.isConfigured()).thenReturn(true);
        when(payWayConfig.getMerchantId()).thenReturn("test.sandbox.merchant");
        when(payWayConfig.getApiKey()).thenReturn("test-sandbox-api-key-12345");
        when(payWayConfig.getBaseUrl()).thenReturn("https://checkout-sandbox.payway.com.kh");
    }

    @Test
    void generateQr_not_configured_throws() {
        when(payWayConfig.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> payWayService.generateQr(
                "PAY001", new BigDecimal("10.00"), "USD", "https://callback.test", 30))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void generateQr_builds_correct_request_fields() {
        PayWayQrResponse successResponse = new PayWayQrResponse(
                "000201...", "data:image/png;base64,abc", "abamobilebank://...",
                10.0, "USD",
                new PayWayQrResponse.PayWayStatus("0", "Success.", "trace-id-123"));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayQrResponse.class)))
                .thenReturn(ResponseEntity.ok(successResponse));

        PayWayQrResponse result = payWayService.generateQr(
                "PAY001", new BigDecimal("10.00"), "USD", "https://callback.test", 30);

        assertThat(result).isNotNull();
        assertThat(result.qrString()).isEqualTo("000201...");
        assertThat(result.status().code()).isEqualTo("0");
    }

    @Test
    void generateQr_wrong_hash_error() {
        PayWayQrResponse errorResponse = new PayWayQrResponse(
                null, null, null, null, null,
                new PayWayQrResponse.PayWayStatus("1", "Wrong Hash.", "trace-id-456"));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayQrResponse.class)))
                .thenReturn(ResponseEntity.ok(errorResponse));

        assertThatThrownBy(() -> payWayService.generateQr(
                "PAY001", new BigDecimal("10.00"), "USD", null, 30))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("Wrong Hash")
                .hasFieldOrPropertyWithValue("payWayCode", "1");
    }

    @Test
    void generateQr_empty_response_throws() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayQrResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> payWayService.generateQr(
                "PAY001", new BigDecimal("10.00"), "USD", null, 30))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generateQr_connection_error_wraps_exception() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayQrResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> payWayService.generateQr(
                "PAY001", new BigDecimal("10.00"), "USD", null, 30))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("Failed to communicate with PayWay");
    }

    @Test
    void generateQr_khqr_payment_option_used() {
        PayWayQrResponse successResponse = new PayWayQrResponse(
                "qr", "img", "deeplink", 5.0, "KHR",
                new PayWayQrResponse.PayWayStatus("0", "Success.", "trace789"));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayQrResponse.class)))
                .thenReturn(ResponseEntity.ok(successResponse));

        PayWayQrResponse result = payWayService.generateQr(
                "PAY002", new BigDecimal("5.00"), "KHR", null, 60);

        assertThat(result).isNotNull();
        assertThat(result.currency()).isEqualTo("KHR");
        assertThat(result.amount()).isEqualTo(5.0);
    }

    @Test
    void verifyCallbackSignature_valid() {
        Map<String, Object> body = new HashMap<>();
        body.put("apv", "502809");
        body.put("merchant_ref", "");
        body.put("return_params", "eyJvcmRlcl9pZCI6IjEyMyJ9");
        body.put("status", "0");
        body.put("tran_id", "5426254593");

        String concatenated = "502809" + "" + "eyJvcmRlcl9pZCI6IjEyMyJ9" + "0" + "5426254593";
        String expectedHmac = payWayService.computeHmacSha512(concatenated, "test-sandbox-api-key-12345");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Payway-Hmac-SHA512", expectedHmac);

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isTrue();
    }

    @Test
    void verifyCallbackSignature_invalid() {
        Map<String, Object> body = new HashMap<>();
        body.put("tran_id", "123");
        body.put("status", "0");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Payway-Hmac-SHA512", "badsignature");

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isFalse();
    }

    @Test
    void verifyCallbackSignature_missing_header() {
        Map<String, Object> body = new HashMap<>();
        body.put("tran_id", "123");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isFalse();
    }

    @Test
    void verifyCallbackSignature_valid_body_hash() throws Exception {
        Map<String, Object> body = abaCallbackBodyWithHash("10.00", "USD", "abapay_khqr");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isTrue();
    }

    @Test
    void verifyCallbackSignature_invalid_body_hash() throws Exception {
        Map<String, Object> body = abaCallbackBodyWithHash("10.00", "USD", "abapay_khqr");
        body.put("hash", Base64.getEncoder().encodeToString("tampered".getBytes(StandardCharsets.UTF_8)));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isFalse();
    }

    @Test
    void verifyCallbackSignature_body_hash_takes_precedence_over_header() throws Exception {
        Map<String, Object> body = abaCallbackBodyWithHash("10.00", "USD", "abapay_khqr");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Payway-Hmac-SHA512", "garbage-through-header");

        assertThat(payWayService.verifyCallbackSignature(body, httpRequest)).isTrue();
    }

    private Map<String, Object> abaCallbackBodyWithHash(String amount, String currency, String paymentOption)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tran_id", "PAY001");
        body.put("status", "0");
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("payment_option", paymentOption);
        body.put("merchant_id", "test.sandbox.merchant");
        body.put("firstname", "");
        body.put("lastname", "");
        body.put("email", "");
        body.put("phone", "");
        body.put("custom_fields", "{}");
        body.put("return_params", "");

        String[] keys = {
            "tran_id", "status", "amount", "currency", "payment_option",
            "merchant_id", "firstname", "lastname", "email", "phone",
            "custom_fields", "return_params"
        };
        TreeMap<String, String> params = new TreeMap<>();
        for (String key : keys) {
            params.put(key, body.get(key) == null ? "" : body.get(key).toString());
        }
        StringBuilder concatenated = new StringBuilder();
        for (String value : params.values()) {
            if (value != null && !value.isBlank()) {
                concatenated.append(value);
            }
        }

        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(
                "test-sandbox-api-key-12345".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        String hash = Base64.getEncoder().encodeToString(
                mac.doFinal(concatenated.toString().getBytes(StandardCharsets.UTF_8)));

        body.put("hash", hash);
        return body;
    }

    @Test
    void checkTransaction_success() {
        PayWayCheckTransactionResponse txResponse = new PayWayCheckTransactionResponse(
                new PayWayCheckTransactionResponse.TransactionData(
                        0, "APPROVED", 100.0, "USD", 100.0, 100.0, 0.0, 0.0, "753786", "2025-02-13 13:55:25"),
                new PayWayCheckTransactionResponse.TransactionStatus("00", "Success!", "PAY001"));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayCheckTransactionResponse.class)))
                .thenReturn(ResponseEntity.ok(txResponse));

        PayWayCheckTransactionResponse result = payWayService.checkTransaction("PAY001");

        assertThat(result).isNotNull();
        assertThat(result.data().paymentStatusCode()).isEqualTo(0);
        assertThat(result.data().paymentStatus()).isEqualTo("APPROVED");
    }

    @Test
    void checkTransaction_not_configured_throws() {
        when(payWayConfig.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> payWayService.checkTransaction("PAY001"))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void checkTransaction_api_error() {
        PayWayCheckTransactionResponse errorResponse = new PayWayCheckTransactionResponse(
                null,
                new PayWayCheckTransactionResponse.TransactionStatus("5", "Invalid hash", null));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PayWayCheckTransactionResponse.class)))
                .thenReturn(ResponseEntity.ok(errorResponse));

        assertThatThrownBy(() -> payWayService.checkTransaction("PAY001"))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("Invalid hash");
    }
}
