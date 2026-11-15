package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidPaymentOperationException;
import com.example.spring_boot_project_api.exception.PaymentMethodNotFoundException;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.PaymentService;
import com.example.spring_boot_project_api.service.UserRoleService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-testing-purposes-only-1234567890",
        "jwt.expiration-ms=900000"
})
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean private PaymentService paymentService;
    @MockitoBean private HouseAccessService houseAccessService;
    @MockitoBean private UserRoleService userRoleService;
    @MockitoBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtUtil.generateToken("johndoe");
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(
                org.springframework.security.core.userdetails.User.builder()
                        .username("johndoe")
                        .password("n/a")
                        .roles("USER")
                        .build());
    }

    @Test
    void createPayment_unauthorized_without_token() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 1, "paymentMethodId": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayment_rejects_invalid_token() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 1, "paymentMethodId": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayment_created_with_valid_token() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, 10L, 1L, "KHQR", "PAY-20260904-000001", "PAY20260904000001",
                new BigDecimal("250.00"), "USD", null, "KHR", PaymentStatus.PENDING,
                "000201010212...", null, LocalDateTime.now());

        when(paymentService.createPayment(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 10, "paymentMethodId": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNo").value("PAY-20260904-000001"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.qrCode").value("000201010212..."));
    }

    @Test
    void createPayment_validation_error_missing_fields() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_404_for_invalid_invoice() throws Exception {
        when(paymentService.createPayment(any()))
                .thenThrow(InvoiceNotFoundException.forId(999L));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 999, "paymentMethodId": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found with id: 999"));
    }

    @Test
    void createPayment_404_for_invalid_payment_method() throws Exception {
        when(paymentService.createPayment(any()))
                .thenThrow(PaymentMethodNotFoundException.forId(999L));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 10, "paymentMethodId": 999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment method not found with id: 999"));
    }

    @Test
    void createPayment_400_for_inactive_payment_method() throws Exception {
        when(paymentService.createPayment(any()))
                .thenThrow(new InvalidPaymentOperationException("Payment method is not active: CASH"));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 10, "paymentMethodId": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment method is not active: CASH"));
    }

    @Test
    void createPayment_400_for_already_paid_invoice() throws Exception {
        when(paymentService.createPayment(any()))
                .thenThrow(new InvalidPaymentOperationException("Invoice is already fully paid"));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId": 10, "paymentMethodId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invoice is already fully paid"));
    }

    @Test
    void callback_returns_200_without_jwt() throws Exception {
        doNothing().when(paymentService).processCallback(any(), any());

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payway-Hmac-SHA512", "some-sig")
                        .content("""
                                {
                                    "tran_id": "PAY20260904000001",
                                    "apv": "502809",
                                    "status": "0",
                                    "return_params": "",
                                    "merchant_ref": ""
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void callback_returns_200_even_on_empty_body() throws Exception {
        doNothing().when(paymentService).processCallback(any(), any());

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payway-Hmac-SHA512", "some-sig")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void callback_returns_200_without_hmac_header() throws Exception {
        doNothing().when(paymentService).processCallback(any(), any());

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tran_id": "123", "status": "0"}
                                """))
                .andExpect(status().isOk());
    }
}
