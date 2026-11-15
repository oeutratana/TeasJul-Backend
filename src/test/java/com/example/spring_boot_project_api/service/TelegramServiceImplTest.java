package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.config.TelegramBotConfig;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.impl.TelegramServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramServiceImplTest {

    private static final String BOT_TOKEN = "123456789:AAFakeTokenForTestsOnly_doNotUse";
    private static final String CHAT_ID = "-1001234567890";

    @Mock private RestTemplate restTemplate;
    @Mock private TelegramBotConfig telegramBotConfig;
    @Mock private OwnerSettingService ownerSettingService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RentalContractRepository rentalContractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private SystemSettingService systemSettingService;

    private TelegramServiceImpl telegramService;

    @BeforeEach
    void setUp() {
        telegramService = new TelegramServiceImpl(
                restTemplate, telegramBotConfig, ownerSettingService, invoiceRepository,
                rentalContractRepository, roomRepository, houseRepository,
                contractTenantRepository, tenantRepository, systemSettingService);
    }

    private TelegramService.PropertySubmission submission() {
        return new TelegramService.PropertySubmission(
                "ROOM#77", "Room / House", "Modern Room", "Sok Owner", "PENDING_APPROVAL",
                LocalDateTime.of(2026, 1, 15, 10, 30));
    }

    private void allowEverything() {
        when(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(true);
        when(systemSettingService.getSuperAdminTelegramSettingsValues())
                .thenReturn(new SuperAdminTelegramSettings(true, BOT_TOKEN, CHAT_ID));
    }

    private String capturedUrl() {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForEntity(url.capture(), any(), eq(String.class));
        return url.getValue();
    }

    @Test
    void propertySubmitted_sendsMessageToSuperAdminDestination() {
        allowEverything();

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        assertThat(capturedUrl()).contains("/sendMessage");
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void propertySubmitted_doesNotSendWhenNotificationSettingsDisabled() {
        when(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(false);

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void propertySubmitted_doesNotSendWhenTelegramChannelDisabled() {
        when(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(false);

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        verify(systemSettingService).isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED);
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void propertySubmitted_doesNotSendWhenSuperAdminTelegramDisabled() {
        when(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(true);
        when(systemSettingService.getSuperAdminTelegramSettingsValues())
                .thenReturn(new SuperAdminTelegramSettings(false, BOT_TOKEN, CHAT_ID));

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void propertySubmitted_doesNotSendWhenNotConfigured() {
        when(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(true);
        when(systemSettingService.getSuperAdminTelegramSettingsValues())
                .thenReturn(SuperAdminTelegramSettings.DISABLED);

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void propertySubmitted_neverFallsBackToOwnerOrEnvironmentCredentials() {
        allowEverything();

        telegramService.sendPropertySubmittedToSuperAdmin(submission());

        // The owner credential lookup and the telegram.bot-token fallback belong
        // to the payment path only; the super admin destination is used as-is.
        verify(ownerSettingService, never()).getTelegramBotToken(any());
        verify(ownerSettingService, never()).getTelegramChatId(any());
    }

    @Test
    void propertySubmitted_telegramFailureIsSwallowed() {
        allowEverything();
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("POST https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage failed"));

        assertThatCode(() -> telegramService.sendPropertySubmittedToSuperAdmin(submission()))
                .doesNotThrowAnyException();
    }

    @Test
    void propertySubmitted_ignoresNullSubmission() {
        assertThatCode(() -> telegramService.sendPropertySubmittedToSuperAdmin(null))
                .doesNotThrowAnyException();

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void paymentCreated_keepsUsingOwnerCredentials() {
        // Regression guard: the owner payment path must keep resolving the owner
        // destination and must not be redirected to the super admin settings.
        when(systemSettingService.isTelegramNotificationEnabled()).thenReturn(true);
        when(ownerSettingService.getTelegramBotToken(9L)).thenReturn(BOT_TOKEN);
        when(ownerSettingService.getTelegramChatId(9L)).thenReturn(CHAT_ID);
        Payment payment = Payment.builder()
                .id(3L)
                .amount(new java.math.BigDecimal("10.00"))
                .owner(User.builder().id(9L).build())
                .build();

        telegramService.sendPaymentCreated(payment);

        verify(ownerSettingService).getTelegramBotToken(9L);
        verify(ownerSettingService).getTelegramChatId(9L);
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
        verify(systemSettingService, never()).getSuperAdminTelegramSettingsValues();
    }
}
