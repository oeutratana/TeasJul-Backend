package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.config.PayWayConfig;
import com.example.spring_boot_project_api.dto.response.PayWayCheckTransactionResponse;
import com.example.spring_boot_project_api.dto.response.PayWayQrResponse;
import com.example.spring_boot_project_api.exception.PayWayIntegrationException;
import com.example.spring_boot_project_api.service.PayWayService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayWayServiceImpl implements PayWayService {

    private static final Logger log = LoggerFactory.getLogger(PayWayServiceImpl.class);

    private static final String QR_API_PATH = "/api/payment-gateway/v1/payments/generate-qr";
    private static final String CHECK_TX_API_PATH = "/api/payment-gateway/v1/payments/check-transaction-2";
    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final String PAYMENT_OPTION_KHQR = "abapay_khqr";
    private static final String QR_IMAGE_TEMPLATE = "template3_color";
    private static final String CALLBACK_HMAC_HEADER = "X-PAYWAY-HMAC-SHA512";
    private static final String CALLBACK_HASH_FIELD = "hash";

    // Official ABA PayWay callback fields used to build the HMAC-SHA512 signature.
    // Order does not matter here: we sort by key and concatenate non-empty values.
    private static final String[] CALLBACK_HASH_KEYS = {
            "tran_id", "status", "amount", "currency", "payment_option",
            "merchant_id", "firstname", "lastname", "email", "phone",
            "custom_fields", "return_params"
    };
    private static final TreeMap<String, String> CALLBACK_HASH_PARAMS;

    static {
        CALLBACK_HASH_PARAMS = new TreeMap<>();
        for (String key : CALLBACK_HASH_KEYS) {
            CALLBACK_HASH_PARAMS.put(key, "");
        }
    }

    private final PayWayConfig payWayConfig;
    private final RestTemplate restTemplate;

    @Override
    public PayWayQrResponse generateQr(String transactionId, BigDecimal amount,
            String currency, String callbackUrl, int lifetimeMinutes) {
        if (!payWayConfig.isConfigured()) {
            throw new PayWayIntegrationException(
                    "PayWay is not configured. Set PAYWAY_MERCHANT_ID and PAYWAY_API_KEY.", (String) null);
        }

        String reqTime = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String encodedCallback = callbackUrl != null && !callbackUrl.isBlank()
                ? Base64.getEncoder().encodeToString(callbackUrl.getBytes(StandardCharsets.UTF_8))
                : "";

        String hashInput = reqTime
                + payWayConfig.getMerchantId()
                + transactionId
                + amount.toPlainString()
                + ""            // items
                + ""            // first_name
                + ""            // last_name
                + ""            // email
                + ""            // phone
                + "purchase"    // purchase_type
                + PAYMENT_OPTION_KHQR
                + encodedCallback
                + ""            // return_deeplink
                + currency
                + ""            // custom_fields
                + ""            // return_params
                + ""            // payout
                + lifetimeMinutes
                + QR_IMAGE_TEMPLATE;

        String hash = computeHmacSha512(hashInput, payWayConfig.getApiKey());

        // Safe hash-field logging (API key is NEVER logged)
        log.info("PayWay QR hash fields (key omitted):");
        log.info("  [01] req_time          = {}", reqTime);
        log.info("  [02] merchant_id       = {}", payWayConfig.getMerchantId());
        log.info("  [03] tran_id           = {}", transactionId);
        log.info("  [04] amount            = {}", amount.toPlainString());
        log.info("  [05] items             = (empty)");
        log.info("  [06] first_name        = (empty)");
        log.info("  [07] last_name         = (empty)");
        log.info("  [08] email             = (empty)");
        log.info("  [09] phone             = (empty)");
        log.info("  [10] purchase_type     = purchase");
        log.info("  [11] payment_option    = {}", PAYMENT_OPTION_KHQR);
        log.info("  [12] callback_url(b64) = {}", encodedCallback);
        log.info("  [13] return_deeplink   = (empty)");
        log.info("  [14] currency          = {}", currency);
        log.info("  [15] custom_fields     = (empty)");
        log.info("  [16] return_params     = (empty)");
        log.info("  [17] payout            = (empty)");
        log.info("  [18] lifetime          = {}", lifetimeMinutes);
        log.info("  [19] qr_image_template = {}", QR_IMAGE_TEMPLATE);
        log.info("PayWay QR concatenated hash input: {}", hashInput);
        log.info("PayWay QR generated hash: {}", hash);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("req_time", reqTime);
        requestBody.put("merchant_id", payWayConfig.getMerchantId());
        requestBody.put("tran_id", transactionId);
        requestBody.put("amount", amount.toPlainString());
        requestBody.put("currency", currency);
        requestBody.put("purchase_type", "purchase");
        requestBody.put("payment_option", PAYMENT_OPTION_KHQR);
        requestBody.put("items", "");
        requestBody.put("first_name", "");
        requestBody.put("last_name", "");
        requestBody.put("email", "");
        requestBody.put("phone", "");
        requestBody.put("callback_url", encodedCallback);
        requestBody.put("return_deeplink", "");
        requestBody.put("custom_fields", "");
        requestBody.put("return_params", "");
        requestBody.put("payout", "");
        requestBody.put("lifetime", lifetimeMinutes);
        requestBody.put("qr_image_template", QR_IMAGE_TEMPLATE);
        requestBody.put("hash", hash);

        log.info("Calling PayWay Generate QR: tranId={}, amount={}, currency={}", transactionId, amount, currency);

        String url = payWayConfig.getBaseUrl() + QR_API_PATH;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<PayWayQrResponse> response = restTemplate.postForEntity(url, request, PayWayQrResponse.class);

            PayWayQrResponse body = response.getBody();
            if (body == null) {
                throw new PayWayIntegrationException("PayWay returned an empty response", (String) null);
            }

            if (body.status() == null) {
                throw new PayWayIntegrationException("PayWay returned a response without status", (String) null);
            }

            if (!"0".equals(body.status().code())) {
                log.error("PayWay QR generation failed: code={}, message={}, traceId={}",
                        body.status().code(), body.status().message(), body.status().traceId());
                throw new PayWayIntegrationException(
                        "PayWay rejected request: " + body.status().message(),
                        body.status().code());
            }

            log.info("PayWay QR generated successfully: tranId={}, traceId={}",
                    transactionId, body.status().traceId());
            return body;

        } catch (PayWayIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayWay API call failed: tranId={}", transactionId, e);
            throw new PayWayIntegrationException("Failed to communicate with PayWay: " + e.getMessage(), e, null);
        }
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, Object> callbackBody, HttpServletRequest request) {
        if (callbackBody == null || callbackBody.isEmpty()) {
            log.warn("PayWay callback has no body");
            return false;
        }

        // ABA PayWay sends the signature as a "hash" field inside the JSON body:
        // HMAC-SHA512 (Base64) of the sorted concatenation of the known callback
        // fields, signed with the merchant API key.
        String bodyHash = valueOf(callbackBody.get(CALLBACK_HASH_FIELD));
        if (!bodyHash.isBlank()) {
            boolean valid = verifyBodyHash(callbackBody, bodyHash);
            if (!valid) {
                log.warn("PayWay callback body hash verification FAILED");
            }
            return valid;
        }

        // Legacy fallback: some gateways send the signature in the
        // X-PAYWAY-HMAC-SHA512 header. Keep supporting it for back-compat.
        String receivedHmac = request.getHeader(CALLBACK_HMAC_HEADER);
        if (receivedHmac == null || receivedHmac.isBlank()) {
            log.warn("PayWay callback missing {} header and no body hash", CALLBACK_HMAC_HEADER);
            return false;
        }

        String concatenatedValues = callbackBody.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> valueOf(entry.getValue()))
                .reduce("", String::concat);

        String expectedHmac = computeHmacSha512(concatenatedValues, payWayConfig.getApiKey());
        return constantTimeEquals(expectedHmac, receivedHmac);
    }

    private boolean verifyBodyHash(Map<String, Object> callbackBody, String receivedHash) {
        TreeMap<String, String> params = new TreeMap<>(CALLBACK_HASH_PARAMS);
        for (String key : CALLBACK_HASH_KEYS) {
            params.put(key, valueOf(callbackBody.get(key)));
        }

        StringBuilder concatenated = new StringBuilder();
        for (String value : params.values()) {
            if (value != null && !value.isBlank()) {
                concatenated.append(value);
            }
        }

        String expectedHmac = computeHmacSha512(concatenated.toString(), payWayConfig.getApiKey());
        return constantTimeEquals(expectedHmac, receivedHash);
    }

    private static String valueOf(Object value) {
        return value != null ? value.toString() : "";
    }

    @Override
    public PayWayCheckTransactionResponse checkTransaction(String transactionId) {
        if (!payWayConfig.isConfigured()) {
            throw new PayWayIntegrationException(
                    "PayWay is not configured.", (String) null);
        }

        String reqTime = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String hashInput = reqTime + payWayConfig.getMerchantId() + transactionId;
        String hash = computeHmacSha512(hashInput, payWayConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("req_time", reqTime);
        requestBody.put("merchant_id", payWayConfig.getMerchantId());
        requestBody.put("tran_id", transactionId);
        requestBody.put("hash", hash);

        String url = payWayConfig.getBaseUrl() + CHECK_TX_API_PATH;

        log.info("Calling PayWay Check Transaction: tranId={}", transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);

            ResponseEntity<PayWayCheckTransactionResponse> response =
                    restTemplate.postForEntity(url, httpRequest, PayWayCheckTransactionResponse.class);

            PayWayCheckTransactionResponse body = response.getBody();
            if (body == null) {
                throw new PayWayIntegrationException("PayWay Check Transaction returned empty response", (String) null);
            }

            if (body.status() == null) {
                throw new PayWayIntegrationException("PayWay Check Transaction returned no status", (String) null);
            }

            String statusCode = body.status().code() != null ? body.status().code().toString() : null;
            if (statusCode == null || !"00".equals(statusCode)) {
                log.error("PayWay Check Transaction failed: code={}, message={}, tranId={}",
                        statusCode, body.status().message(), transactionId);
                throw new PayWayIntegrationException(
                        "PayWay Check Transaction failed: " + body.status().message(), statusCode);
            }

            log.info("PayWay Check Transaction success: tranId={}, status={}",
                    transactionId, body.data() != null ? body.data().paymentStatus() : "unknown");
            return body;

        } catch (PayWayIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayWay Check Transaction API call failed: tranId={}", transactionId, e);
            throw new PayWayIntegrationException("Failed to communicate with PayWay: " + e.getMessage(), e, null);
        }
    }

    public String computeHmacSha512(String data, String apiKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    apiKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new PayWayIntegrationException("Failed to compute PayWay hash", e, null);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
