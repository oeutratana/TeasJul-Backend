package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.example.spring_boot_project_api.config.PayWayConfig;
import com.example.spring_boot_project_api.dto.request.CreatePaymentRequest;
import com.example.spring_boot_project_api.dto.request.RecordManualPaymentRequest;
import com.example.spring_boot_project_api.dto.response.PayWayCheckTransactionResponse;
import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.dto.response.PayWayQrResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentMethodStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidPaymentOperationException;
import com.example.spring_boot_project_api.exception.PaymentMethodNotFoundException;
import com.example.spring_boot_project_api.exception.PaymentNotFoundException;
import com.example.spring_boot_project_api.exception.PayWayIntegrationException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentMethodRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.impl.PaymentServiceImpl;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PayWayService payWayService;
    @Mock private HouseAccessService houseAccessService;
    @Mock private SecurityUtils securityUtils;
    @Mock private PayWayConfig payWayConfig;
    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private RentalContractRepository rentalContractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private NotificationService notificationService;
    @Mock private OwnerSettingService ownerSettingService;
    @Mock private TelegramService telegramService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private User owner;
    private Invoice invoice;
    private PaymentMethod khqrMethod;
    private MockHttpServletRequest callbackRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner1").email("owner1@test.com").build();

        invoice = Invoice.builder()
                .id(10L)
                .owner(owner)
                .invoiceNo("INV-2026-0001")
                .contractId(100L)
                .billingYear(2026)
                .billingMonth(9)
                .subtotal(new BigDecimal("250.00"))
                .discount(BigDecimal.ZERO)
                .lateFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250.00"))
                .status(InvoiceStatus.PENDING)
                .build();

        khqrMethod = PaymentMethod.builder()
                .id(1L)
                .owner(owner)
                .name("KHQR")
                .code("KHQR")
                .status(PaymentMethodStatus.ACTIVE)
                .build();

        callbackRequest = new MockHttpServletRequest();
        callbackRequest.addHeader("X-Payway-Hmac-SHA512", "valid-sig");

        when(payWayConfig.isConfigured()).thenReturn(true);
        when(payWayConfig.getMerchantId()).thenReturn("test.merchant");
        when(payWayConfig.getApiKey()).thenReturn("test-api-key");
        when(payWayConfig.getCallbackUrl()).thenReturn("https://callback.test/pay");
        when(ownerSettingService.getExchangeRate(any())).thenReturn(new BigDecimal("4100"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authAsOwner() {
        Authentication auth = new TestingAuthenticationToken("owner1", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityUtils.getCurrentUserId()).thenReturn(owner.getId());
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
    }

    private Payment stubPaymentSave() {
        Payment saved = Payment.builder()
                .id(1L)
                .owner(owner)
                .invoiceId(invoice.getId())
                .paymentMethod(khqrMethod)
                .paymentNo("PAY-20260904-000001")
                .transactionId("PAY20260904000001")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), p.getInvoiceId(), p.getPaymentMethod().getId(),
                    p.getPaymentMethod().getName(), p.getPaymentNo(), p.getTransactionId(),
                    p.getAmount(), p.getCurrency(), p.getExchangeRate(), p.getExchangeRateTo(),
                    p.getStatus(), p.getQrCode(),
                    p.getPaidAt(), p.getCreatedAt());
        });
        return saved;
    }

    private Map<String, Object> buildCallback(String tranId) {
        Map<String, Object> callback = new HashMap<>();
        callback.put("tran_id", tranId);
        callback.put("apv", "502809");
        callback.put("status", "0");
        callback.put("return_params", "");
        callback.put("merchant_ref", "");
        return callback;
    }

    private PayWayCheckTransactionResponse buildCheckTxResponse(String tranId, int statusCode,
            double amount, String currency, String paywayStatus) {
        return new PayWayCheckTransactionResponse(
                new PayWayCheckTransactionResponse.TransactionData(
                        statusCode, paywayStatus, amount, currency, amount, amount, 0.0, 0.0, "753786", "2025-01-01"),
                new PayWayCheckTransactionResponse.TransactionStatus("00", "Success!", tranId));
    }

    // ============ CREATE PAYMENT TESTS ============

    @Test
    void createPayment_success() {
        authAsOwner();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of());
        when(paymentRepository.count()).thenReturn(0L);

        Payment saved = stubPaymentSave();

        PayWayQrResponse qrResponse = new PayWayQrResponse(
                "000201010212...",
                "data:image/png;base64,abc...",
                "abamobilebank://...",
                250.0, "USD",
                new PayWayQrResponse.PayWayStatus("0", "Success.", "trace123"));
        when(payWayService.generateQr(any(), eq(new BigDecimal("250.00")),
                eq("USD"), any(), anyInt())).thenReturn(qrResponse);

        PaymentResponse response = paymentService.createPayment(new CreatePaymentRequest(10L, 1L));

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.qrCode()).isEqualTo("000201010212...");
    }

    @Test
    void createPayment_tenant_success() {
        User tenantUser = User.builder().id(50L).username("tenant1").email("tenant@test.com").build();
        Tenant tenant = Tenant.builder().id(200L).user(tenantUser).build();

        Authentication auth = new TestingAuthenticationToken("tenant1", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityUtils.getCurrentUserId()).thenReturn(50L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(tenantContextResolver.linkedTenantOf(50L)).thenReturn(tenant);
        when(contractTenantRepository.existsByContractIdAndTenantId(100L, 200L)).thenReturn(true);
        when(paymentMethodRepository.findFirstByOwnerIdAndStatus(1L, PaymentMethodStatus.ACTIVE))
                .thenReturn(Optional.of(khqrMethod));
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of());
        when(paymentRepository.count()).thenReturn(0L);

        Payment saved = stubPaymentSave();
        PayWayQrResponse qrResponse = new PayWayQrResponse(
                "000201010212...",
                "data:image/png;base64,abc...",
                "abamobilebank://...",
                250.0, "USD",
                new PayWayQrResponse.PayWayStatus("0", "Success.", "trace123"));
        when(payWayService.generateQr(any(), any(), eq("USD"), any(), anyInt())).thenReturn(qrResponse);

        PaymentResponse response = paymentService.createPayment(new CreatePaymentRequest(10L, null));

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPayment_rejects_cross_owner() {
        authAsOwner();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenThrow(
                new AccessDeniedException("You do not have permission to access this resource"));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(10L, 1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createPayment_invoice_not_found() {
        authAsOwner();

        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(99L, 1L)))
                .isInstanceOf(InvoiceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createPayment_payment_method_not_found() {
        authAsOwner();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(10L, 99L)))
                .isInstanceOf(PaymentMethodNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createPayment_rejects_inactive_payment_method() {
        authAsOwner();

        PaymentMethod inactiveMethod = PaymentMethod.builder()
                .id(2L).owner(owner).name("CASH").code("CASH")
                .status(PaymentMethodStatus.INACTIVE).build();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(2L)).thenReturn(Optional.of(inactiveMethod));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(10L, 2L)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void createPayment_rejects_already_paid_invoice() {
        authAsOwner();

        Invoice paidInvoice = Invoice.builder()
                .id(11L).owner(owner).status(InvoiceStatus.PAID)
                .totalAmount(new BigDecimal("100.00")).discount(BigDecimal.ZERO).build();

        when(invoiceRepository.findById(11L)).thenReturn(Optional.of(paidInvoice));
        when(houseAccessService.resolveInvoiceHouseId(11L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(11L, 1L)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("already fully paid");
    }

    @Test
    void createPayment_rejects_cancelled_invoice() {
        authAsOwner();

        Invoice cancelledInvoice = Invoice.builder()
                .id(12L).owner(owner).status(InvoiceStatus.CANCELLED)
                .totalAmount(new BigDecimal("100.00")).discount(BigDecimal.ZERO).build();

        when(invoiceRepository.findById(12L)).thenReturn(Optional.of(cancelledInvoice));
        when(houseAccessService.resolveInvoiceHouseId(12L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(12L, 1L)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void createPayment_rejects_duplicate_active_payment() {
        authAsOwner();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        Payment activePayment = Payment.builder()
                .id(2L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000002")
                .transactionId("PAY20260904000002").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of(activePayment));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(10L, 1L)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("active payment already exists");
    }

    @Test
    void createPayment_expires_stale_active_payment_before_rejecting() {
        authAsOwner();

        Payment stalePayment = Payment.builder()
                .id(2L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260903-000009")
                .transactionId("PAY20260903000009").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(5)).build();
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of(stalePayment))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), p.getInvoiceId(), p.getPaymentMethod().getId(),
                    p.getPaymentMethod().getName(), p.getPaymentNo(), p.getTransactionId(),
                    p.getAmount(), p.getCurrency(), p.getExchangeRate(), p.getExchangeRateTo(),
                    p.getStatus(), p.getQrCode(),
                    p.getPaidAt(), p.getCreatedAt());
        });

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(paymentRepository.count()).thenReturn(0L);
        when(payWayService.generateQr(any(), any(), eq("USD"), any(), anyInt())).thenReturn(
                new PayWayQrResponse("qr", "img", "deeplink", 250.0, "USD",
                        new PayWayQrResponse.PayWayStatus("0", "Success.", "trace")));

        PaymentResponse response = paymentService.createPayment(new CreatePaymentRequest(10L, 1L));

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(stalePayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce())
                .save(argThat(p -> p == stalePayment && p.getStatus() == PaymentStatus.EXPIRED));
    }

    @Test
    void createPayment_rejects_zero_outstanding() {
        authAsOwner();

        Invoice zeroInvoice = Invoice.builder()
                .id(13L).owner(owner).status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO).build();

        when(invoiceRepository.findById(13L)).thenReturn(Optional.of(zeroInvoice));
        when(houseAccessService.resolveInvoiceHouseId(13L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(paymentRepository.findByInvoiceIdAndStatusIn(13L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(13L, 1L)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("no outstanding amount");
    }

    @Test
    void createPayment_records_failed_status_on_payway_error() {
        authAsOwner();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of());
        when(paymentRepository.count()).thenReturn(0L);

        stubPaymentSave();

        when(payWayService.generateQr(any(), any(), any(), any(), anyInt()))
                .thenThrow(new PayWayIntegrationException("PayWay rejected request: Wrong Hash", "1"));

        assertThatThrownBy(() -> paymentService.createPayment(new CreatePaymentRequest(10L, 1L)))
                .isInstanceOf(PayWayIntegrationException.class)
                .hasMessageContaining("Wrong Hash");

        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void createPayment_manager_can_create_for_assigned_house() {
        User manager = User.builder().id(2L).username("mgr1").email("mgr1@test.com").build();
        Authentication auth = new TestingAuthenticationToken("mgr1", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityUtils.getCurrentUserId()).thenReturn(2L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);

        PaymentMethod managerKhqr = PaymentMethod.builder()
                .id(2L).owner(manager).name("KHQR").code("KHQR")
                .status(PaymentMethodStatus.ACTIVE).build();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(paymentMethodRepository.findById(2L)).thenReturn(Optional.of(managerKhqr));
        when(paymentRepository.findByInvoiceIdAndStatusIn(10L, List.of(PaymentStatus.PENDING)))
                .thenReturn(List.of());
        when(paymentRepository.count()).thenReturn(0L);

        Payment saved = Payment.builder()
                .id(1L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(managerKhqr).paymentNo("PAY-20260904-000001")
                .transactionId("PAY20260904000001").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).createdAt(LocalDateTime.now()).build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), p.getInvoiceId(), p.getPaymentMethod().getId(),
                    p.getPaymentMethod().getName(), p.getPaymentNo(), p.getTransactionId(),
                    p.getAmount(), p.getCurrency(), p.getExchangeRate(), p.getExchangeRateTo(),
                    p.getStatus(), p.getQrCode(),
                    p.getPaidAt(), p.getCreatedAt());
        });

        PayWayQrResponse qrResponse = new PayWayQrResponse(
                "qrString123", "data:image/png;base64,abc", "deeplink",
                250.0, "USD",
                new PayWayQrResponse.PayWayStatus("0", "Success.", "trace456"));
        when(payWayService.generateQr(any(), any(), any(), any(), anyInt())).thenReturn(qrResponse);

        PaymentResponse response = paymentService.createPayment(new CreatePaymentRequest(10L, 2L));

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    }

    // ============ CALLBACK TESTS ============

    @Test
    void processCallback_success_marks_paid_and_updates_invoice() {
        Payment pendingPayment = Payment.builder()
                .id(1L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000001")
                .transactionId("PAY20260904000001").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000001"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000001", 0, 250.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000001")).thenReturn(txResponse);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("250.00"));

        paymentService.processCallback(buildCallback("PAY20260904000001"), callbackRequest);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PAID && p.getPaidAt() != null));
        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PAID));
    }

    @Test
    void processCallback_invalid_signature_still_reconciles_via_check_transaction() {
        Payment pendingPayment = Payment.builder()
                .id(4L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000004")
                .transactionId("PAY20260904000004").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(false);
        when(paymentRepository.findByTransactionId("PAY20260904000004"))
                .thenReturn(Optional.of(pendingPayment));
        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000004", 0, 250.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000004")).thenReturn(txResponse);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("250.00"));

        paymentService.processCallback(buildCallback("PAY20260904000004"), callbackRequest);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PAID));
    }

    @Test
    void processCallback_empty_body_ignored() {
        paymentService.processCallback(new HashMap<>(), callbackRequest);

        verify(paymentRepository, never()).findByTransactionId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_expired_payment_confirmed_paid() {
        Payment expiredPayment = Payment.builder()
                .id(13L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000013")
                .transactionId("PAY20260904000013").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.EXPIRED).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000013"))
                .thenReturn(Optional.of(expiredPayment));
        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000013", 0, 250.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000013")).thenReturn(txResponse);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("250.00"));

        paymentService.processCallback(buildCallback("PAY20260904000013"), callbackRequest);

        verify(paymentRepository).save(argThat(p ->
                p.getStatus() == PaymentStatus.PAID && p.getId().equals(13L)));
        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PAID));
    }

    @Test
    void processCallback_unknown_transaction_ignored() {
        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("UNKNOWN")).thenReturn(Optional.empty());

        paymentService.processCallback(buildCallback("UNKNOWN"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_already_paid_idempotent() {
        Payment paidPayment = Payment.builder()
                .id(3L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000003")
                .transactionId("PAY20260904000003").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PAID).paidAt(LocalDateTime.now()).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000003"))
                .thenReturn(Optional.of(paidPayment));

        paymentService.processCallback(buildCallback("PAY20260904000003"), callbackRequest);

        verify(paymentRepository, never()).save(any());
        verify(payWayService, never()).checkTransaction(any());
    }

    @Test
    void processCallback_check_transaction_fails_payment_unchanged() {
        Payment pendingPayment = Payment.builder()
                .id(5L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000005")
                .transactionId("PAY20260904000005").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000005"))
                .thenReturn(Optional.of(pendingPayment));
        when(payWayService.checkTransaction("PAY20260904000005"))
                .thenThrow(new PayWayIntegrationException("Check Transaction failed", "5"));

        paymentService.processCallback(buildCallback("PAY20260904000005"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_amount_mismatch_rejected() {
        Payment pendingPayment = Payment.builder()
                .id(6L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000006")
                .transactionId("PAY20260904000006").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000006"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000006", 0, 100.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000006")).thenReturn(txResponse);

        paymentService.processCallback(buildCallback("PAY20260904000006"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_currency_mismatch_rejected() {
        Payment pendingPayment = Payment.builder()
                .id(7L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000007")
                .transactionId("PAY20260904000007").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000007"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000007", 0, 250.0, "KHR", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000007")).thenReturn(txResponse);

        paymentService.processCallback(buildCallback("PAY20260904000007"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_transaction_id_mismatch_rejected() {
        Payment pendingPayment = Payment.builder()
                .id(8L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000008")
                .transactionId("PAY20260904000008").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000008"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "DIFFERENT_ID", 0, 250.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000008")).thenReturn(txResponse);

        paymentService.processCallback(buildCallback("PAY20260904000008"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_not_approved_marks_failed() {
        Payment pendingPayment = Payment.builder()
                .id(9L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000009")
                .transactionId("PAY20260904000009").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000009"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000009", 3, 250.0, "USD", "DECLINED");
        when(payWayService.checkTransaction("PAY20260904000009")).thenReturn(txResponse);

        paymentService.processCallback(buildCallback("PAY20260904000009"), callbackRequest);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
    }

    @Test
    void processCallback_invoice_partial_when_underpaid() {
        Payment pendingPayment = Payment.builder()
                .id(10L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000010")
                .transactionId("PAY20260904000010").amount(new BigDecimal("100.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000010"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = buildCheckTxResponse(
                "PAY20260904000010", 0, 100.0, "USD", "APPROVED");
        when(payWayService.checkTransaction("PAY20260904000010")).thenReturn(txResponse);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("100.00"));

        paymentService.processCallback(buildCallback("PAY20260904000010"), callbackRequest);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PAID));
        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PARTIAL));
    }

    @Test
    void processCallback_missing_tran_id_ignored() {
        Map<String, Object> callback = new HashMap<>();
        callback.put("apv", "502809");
        callback.put("status", "0");

        paymentService.processCallback(callback, callbackRequest);

        verify(paymentRepository, never()).findByTransactionId(any());
    }

    @Test
    void processCallback_already_failed_payment_not_promoted_to_paid() {
        Payment failedPayment = Payment.builder()
                .id(11L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000011")
                .transactionId("PAY20260904000011").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.FAILED).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000011"))
                .thenReturn(Optional.of(failedPayment));

        paymentService.processCallback(buildCallback("PAY20260904000011"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processCallback_check_transaction_null_data_ignored() {
        Payment pendingPayment = Payment.builder()
                .id(12L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000012")
                .transactionId("PAY20260904000012").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(payWayService.verifyCallbackSignature(any(), eq(callbackRequest))).thenReturn(true);
        when(paymentRepository.findByTransactionId("PAY20260904000012"))
                .thenReturn(Optional.of(pendingPayment));

        PayWayCheckTransactionResponse txResponse = new PayWayCheckTransactionResponse(
                null,
                new PayWayCheckTransactionResponse.TransactionStatus("00", "Success!", "PAY20260904000012"));
        when(payWayService.checkTransaction("PAY20260904000012")).thenReturn(txResponse);

        paymentService.processCallback(buildCallback("PAY20260904000012"), callbackRequest);

        verify(paymentRepository, never()).save(any());
    }

    // ============ PAYMENT STATUS TESTS ============

    @Test
    void getPaymentStatus_reconciles_and_returns_paid() {
        authAsOwner();

        Payment pendingPayment = Payment.builder()
                .id(14L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000014")
                .transactionId("PAY20260904000014").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();

        when(paymentRepository.findByTransactionId("PAY20260904000014"))
                .thenReturn(Optional.of(pendingPayment));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(payWayService.checkTransaction("PAY20260904000014"))
                .thenReturn(buildCheckTxResponse("PAY20260904000014", 0, 250.0, "USD", "APPROVED"));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("250.00"));

        var response = paymentService.getPaymentStatus("PAY20260904000014");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.invoiceNo()).isEqualTo("INV-2026-0001");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void getPaymentStatus_expired_payment_reconciled_to_paid() {
        authAsOwner();

        Payment expiredPayment = Payment.builder()
                .id(15L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000015")
                .transactionId("PAY20260904000015").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.EXPIRED).build();

        when(paymentRepository.findByTransactionId("PAY20260904000015"))
                .thenReturn(Optional.of(expiredPayment));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(payWayService.checkTransaction("PAY20260904000015"))
                .thenReturn(buildCheckTxResponse("PAY20260904000015", 0, 250.0, "USD", "APPROVED"));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(new BigDecimal("250.00"));

        var response = paymentService.getPaymentStatus("PAY20260904000015");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void getPaymentStatus_paid_is_terminal_no_reconciliation() {
        authAsOwner();

        Payment paidPayment = Payment.builder()
                .id(16L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000016")
                .transactionId("PAY20260904000016").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PAID).paidAt(LocalDateTime.now()).build();

        when(paymentRepository.findByTransactionId("PAY20260904000016"))
                .thenReturn(Optional.of(paidPayment));
        when(houseAccessService.resolveInvoiceHouseId(10L)).thenReturn(20L);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));

        var response = paymentService.getPaymentStatus("PAY20260904000016");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(payWayService, never()).checkTransaction(any());
    }

    @Test
    void getPaymentStatus_tenant_denied_when_not_linked() {
        Authentication auth = new TestingAuthenticationToken("tenant1", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityUtils.getCurrentUserId()).thenReturn(50L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        Payment pendingPayment = Payment.builder()
                .id(17L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000017")
                .transactionId("PAY20260904000017").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByTransactionId("PAY20260904000017"))
                .thenReturn(Optional.of(pendingPayment));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(tenantContextResolver.linkedTenantOf(50L)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.getPaymentStatus("PAY20260904000017"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getPaymentStatus_unknown_transaction_not_found() {
        authAsOwner();

        when(paymentRepository.findByTransactionId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("UNKNOWN"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ============ MANUAL PAYMENT TESTS ============

    private void stubManualPaymentFlow(boolean invoiceEndsPaid) {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findPaidTotalByInvoiceId(10L))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(invoiceEndsPaid ? new BigDecimal("250.00") : new BigDecimal("100.00"));
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), p.getInvoiceId(), p.getPaymentMethod().getId(),
                    p.getPaymentMethod().getName(), p.getPaymentNo(), p.getTransactionId(),
                    p.getAmount(), p.getCurrency(), p.getExchangeRate(), p.getExchangeRateTo(),
                    p.getStatus(), p.getQrCode(),
                    p.getPaidAt(), p.getCreatedAt());
        });
    }

    @Test
    void recordManualPayment_full_payment_marks_invoice_paid() {
        authAsOwner();
        stubManualPaymentFlow(true);

        var response = paymentService.recordManualPayment(new RecordManualPaymentRequest(10L, 1L, new BigDecimal("250.00"), null));

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PAID));
    }

    @Test
    void recordManualPayment_partial_marks_invoice_partial() {
        authAsOwner();
        stubManualPaymentFlow(false);

        var response = paymentService.recordManualPayment(new RecordManualPaymentRequest(10L, 1L, new BigDecimal("100.00"), null));

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PARTIAL));
    }

    @Test
    void recordManualPayment_amount_exceeds_outstanding_rejected() {
        authAsOwner();

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(khqrMethod));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findPaidTotalByInvoiceId(10L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentService.recordManualPayment(
                new RecordManualPaymentRequest(10L, 1L, new BigDecimal("999.00"), null)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void recordManualPayment_tenant_denied() {
        Authentication auth = new TestingAuthenticationToken("tenant1", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityUtils.getCurrentUserId()).thenReturn(50L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.recordManualPayment(
                new RecordManualPaymentRequest(10L, 1L, new BigDecimal("100.00"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recordManualPayment_custom_paidAt_used() {
        authAsOwner();
        stubManualPaymentFlow(true);

        var paidAt = LocalDateTime.of(2026, 9, 1, 10, 30);

        var response = paymentService.recordManualPayment(
                new RecordManualPaymentRequest(10L, 1L, new BigDecimal("250.00"), paidAt));

        assertThat(response.paidAt()).isEqualTo(paidAt);
    }

    // ============ PAYMENT LIST TESTS ============

    @Test
    void getPayments_owner_returns_mapped_payments() {
        authAsOwner();

        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(owner.getId()));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(400L));

        Payment paidPayment = Payment.builder()
                .id(30L).owner(owner).invoiceId(invoice.getId())
                .paymentMethod(khqrMethod).paymentNo("PAY-20260904-000030")
                .transactionId("PAY20260904000030").amount(new BigDecimal("250.00"))
                .currency("USD").status(PaymentStatus.PAID).paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).build();

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paidPayment), pageable, 1));

        when(invoiceRepository.findAllById(List.of(10L))).thenReturn(List.of(invoice));

        RentalContract contract = RentalContract.builder().id(100L).roomId(300L).build();
        when(rentalContractRepository.findAllById(List.of(100L))).thenReturn(List.of(contract));

        Room room = new Room();
        room.setId(300L);
        room.setHouseId(400L);
        room.setRoomNumber("A101");
        when(roomRepository.findAllById(List.of(300L))).thenReturn(List.of(room));

        House house = House.builder().id(400L).name("Orange Villa").build();
        when(houseRepository.findAllById(List.of(400L))).thenReturn(List.of(house));

        ContractTenant link = ContractTenant.builder()
                .id(1L).contractId(100L).tenantId(500L).isPrimary(true).build();
        when(contractTenantRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(link));

        Tenant tenant = Tenant.builder().id(500L).firstName("Sok").lastName("Dara").build();
        when(tenantRepository.findAllById(List.of(500L))).thenReturn(List.of(tenant));

        var page = paymentService.getPayments(null, null, null, null, pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        var item = page.content().get(0);
        assertThat(item.paymentNo()).isEqualTo("PAY-20260904-000030");
        assertThat(item.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(item.invoiceNo()).isEqualTo("INV-2026-0001");
        assertThat(item.houseId()).isEqualTo(400L);
        assertThat(item.houseName()).isEqualTo("Orange Villa");
        assertThat(item.roomNumber()).isEqualTo("A101");
        assertThat(item.tenantName()).isEqualTo("Sok Dara");
        assertThat(item.paymentMethodName()).isEqualTo("KHQR");
        assertThat(item.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void getPayments_no_accessible_scope_returns_empty_page() {
        authAsOwner();

        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of());

        var page = paymentService.getPayments(null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void getPayments_house_id_enforces_access() {
        authAsOwner();

        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(owner.getId()));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(400L));

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var page = paymentService.getPayments(400L, PaymentStatus.PAID, null, null, pageable);

        verify(houseAccessService).requireHouseAccess(400L);
        assertThat(page.content()).isEmpty();
    }
}
