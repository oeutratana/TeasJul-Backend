package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.UpdateLoginRateLimitSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSuperAdminTelegramSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemNotificationSettingsRequest;
import com.example.spring_boot_project_api.dto.response.LoginRateLimitSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SuperAdminTelegramSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SystemNotificationSettingsResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.model.SystemSetting;
import com.example.spring_boot_project_api.repository.SystemSettingRepository;
import com.example.spring_boot_project_api.service.impl.SystemSettingServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SystemSettingServiceImpl systemSettingService;

    private SystemSetting defaultSetting;

    @BeforeEach
    void setUp() {
        defaultSetting = SystemSetting.builder()
                .id(1L)
                .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .settingValue("{\"systemNotificationsEnabled\":true,\"inAppNotificationsEnabled\":true,\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":true,\"invoiceCreatedEnabled\":true,\"paymentCreatedEnabled\":true,\"paymentSuccessEnabled\":true,\"paymentFailedEnabled\":true,\"maintenanceRequestEnabled\":true,\"maintenanceStatusEnabled\":true,\"contractExpiringEnabled\":true,\"roomAssignedEnabled\":true}")
                .description("System-wide notification preferences")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getNotificationSettings_returnsConfig_whenSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(defaultSetting));

        SystemNotificationSettingsResponse response = systemSettingService.getNotificationSettings();

        assertThat(response).isNotNull();
        assertThat(response.systemNotificationsEnabled()).isTrue();
        assertThat(response.inAppNotificationsEnabled()).isTrue();
        assertThat(response.telegramNotificationsEnabled()).isTrue();
        assertThat(response.propertySubmittedEnabled()).isTrue();
    }

    @Test
    void getNotificationSettings_throwsAccessDenied_whenNotSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemSettingService.getNotificationSettings())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can manage system notification settings");
    }

    @Test
    void getNotificationSettings_createsDefaults_whenNoneExist() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.empty());
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemNotificationSettingsResponse response = systemSettingService.getNotificationSettings();

        assertThat(response).isNotNull();
        assertThat(response.systemNotificationsEnabled()).isTrue();
        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    @Test
    void updateNotificationSettings_updatesValues_whenSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(defaultSetting));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSystemNotificationSettingsRequest request = new UpdateSystemNotificationSettingsRequest(
                true,
                true,
                false, // telegram disabled
                false, // property submitted disabled
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );

        SystemNotificationSettingsResponse response = systemSettingService.updateNotificationSettings(request);

        assertThat(response).isNotNull();
        assertThat(response.telegramNotificationsEnabled()).isFalse();
        assertThat(response.propertySubmittedEnabled()).isFalse();

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).contains("\"telegramNotificationsEnabled\":false");
        assertThat(captor.getValue().getSettingValue()).contains("\"propertySubmittedEnabled\":false");
    }

    @Test
    void isNotificationTypeEnabled_returnsTrue_whenEnabled() {
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(defaultSetting));

        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).isTrue();
        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.INVOICE_CREATED)).isTrue();
    }

    @Test
    void isNotificationTypeEnabled_returnsFalse_whenMasterSwitchDisabled() {
        SystemSetting disabledSetting = SystemSetting.builder()
                .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .settingValue("{\"systemNotificationsEnabled\":false,\"inAppNotificationsEnabled\":true,\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":true}")
                .build();
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(disabledSetting));

        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).isFalse();
    }

    @Test
    void isNotificationTypeEnabled_returnsFalse_whenInAppDisabled() {
        SystemSetting disabledSetting = SystemSetting.builder()
                .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .settingValue("{\"systemNotificationsEnabled\":true,\"inAppNotificationsEnabled\":false,\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":true}")
                .build();
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(disabledSetting));

        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).isFalse();
    }

    @Test
    void isNotificationTypeEnabled_returnsFalse_whenSpecificTypeDisabled() {
        SystemSetting disabledSetting = SystemSetting.builder()
                .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .settingValue("{\"systemNotificationsEnabled\":true,\"inAppNotificationsEnabled\":true,\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":false,\"invoiceCreatedEnabled\":true}")
                .build();
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(disabledSetting));

        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).isFalse();
        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.INVOICE_CREATED)).isTrue();
    }

    @Test
    void isTelegramNotificationEnabled_returnsCorrectStatus() {
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(defaultSetting));
        assertThat(systemSettingService.isTelegramNotificationEnabled()).isTrue();

        SystemSetting disabledSetting = SystemSetting.builder()
                .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .settingValue("{\"systemNotificationsEnabled\":true,\"telegramNotificationsEnabled\":false}")
                .build();
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(disabledSetting));
        assertThat(systemSettingService.isTelegramNotificationEnabled()).isFalse();
    }

    private SystemSetting loginSetting(String key, String value) {
        return SystemSetting.builder()
                .id(1L)
                .settingKey(key)
                .settingValue(value)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void stubLoginRows(String enabled, String attempts, String lockout) {
        lenient().when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_LOGIN_RATE_LIMIT_ENABLED))
                .thenReturn(enabled == null ? Optional.empty()
                        : Optional.of(loginSetting(SystemSettingServiceImpl.KEY_LOGIN_RATE_LIMIT_ENABLED, enabled)));
        lenient().when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_LOGIN_MAX_FAILED_ATTEMPTS))
                .thenReturn(attempts == null ? Optional.empty()
                        : Optional.of(loginSetting(SystemSettingServiceImpl.KEY_LOGIN_MAX_FAILED_ATTEMPTS, attempts)));
        lenient().when(systemSettingRepository
                .findBySettingKey(SystemSettingServiceImpl.KEY_LOGIN_LOCKOUT_DURATION_MINUTES))
                .thenReturn(lockout == null ? Optional.empty()
                        : Optional.of(loginSetting(SystemSettingServiceImpl.KEY_LOGIN_LOCKOUT_DURATION_MINUTES, lockout)));
    }

    @Test
    void getLoginRateLimitSettings_returnsStoredValues_whenSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows("true", "8", "30");

        LoginRateLimitSettingsResponse response = systemSettingService.getLoginRateLimitSettings();

        assertThat(response.loginRateLimitEnabled()).isTrue();
        assertThat(response.maxFailedAttempts()).isEqualTo(8);
        assertThat(response.lockoutDurationMinutes()).isEqualTo(30);
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void getLoginRateLimitSettings_throwsAccessDenied_whenNotSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemSettingService.getLoginRateLimitSettings())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can manage login rate limit settings");
    }

    @Test
    void getLoginRateLimitSettings_fallsBackToShippedDefaults_whenRowsMissing() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows(null, null, null);

        LoginRateLimitSettingsResponse response = systemSettingService.getLoginRateLimitSettings();

        assertThat(response.loginRateLimitEnabled()).isTrue();
        assertThat(response.maxFailedAttempts()).isEqualTo(5);
        assertThat(response.lockoutDurationMinutes()).isEqualTo(15);
    }

    @Test
    void getLoginRateLimitSettings_fallsBackToDefaults_whenStoredValueIsCorrupt() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows("maybe", "5.5", "999999");

        LoginRateLimitSettingsResponse response = systemSettingService.getLoginRateLimitSettings();

        assertThat(response.loginRateLimitEnabled()).isTrue();
        assertThat(response.maxFailedAttempts()).isEqualTo(5);
        assertThat(response.lockoutDurationMinutes()).isEqualTo(15);
    }

    @Test
    void updateLoginRateLimitSettings_persistsAllThreeKeys_whenSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows("true", "5", "15");
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRateLimitSettingsResponse response = systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(false, 9, 45));

        assertThat(response.loginRateLimitEnabled()).isFalse();
        assertThat(response.maxFailedAttempts()).isEqualTo(9);
        assertThat(response.lockoutDurationMinutes()).isEqualTo(45);

        Map<String, String> saved = captureSavedLoginValues();
        assertThat(saved).containsEntry(SystemSettingServiceImpl.KEY_LOGIN_RATE_LIMIT_ENABLED, "false");
        assertThat(saved).containsEntry(SystemSettingServiceImpl.KEY_LOGIN_MAX_FAILED_ATTEMPTS, "9");
        assertThat(saved).containsEntry(SystemSettingServiceImpl.KEY_LOGIN_LOCKOUT_DURATION_MINUTES, "45");
    }

    @Test
    void updateLoginRateLimitSettings_createsRowsOnFirstSave() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows(null, null, null);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRateLimitSettingsResponse response = systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 5, 15));

        assertThat(response.maxFailedAttempts()).isEqualTo(5);
        assertThat(captureSavedLoginValues())
                .containsEntry(SystemSettingServiceImpl.KEY_LOGIN_MAX_FAILED_ATTEMPTS, "5");
    }

    @Test
    void updateLoginRateLimitSettings_throwsAccessDenied_whenNotSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 5, 15)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can manage login rate limit settings");
    }

    @Test
    void updateLoginRateLimitSettings_rejectsNullAndOutOfRangeValues() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubLoginRows("true", "5", "15");

        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(null, 5, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loginRateLimitEnabled");
        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 0, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFailedAttempts must be between 1 and 100");
        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, -1, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFailedAttempts must be between 1 and 100");
        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 101, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFailedAttempts must be between 1 and 100");
        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 5, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockoutDurationMinutes must be between 1 and 1440");
        assertThatThrownBy(() -> systemSettingService.updateLoginRateLimitSettings(
                new UpdateLoginRateLimitSettingsRequest(true, 5, 1441)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockoutDurationMinutes must be between 1 and 1440");

        verify(systemSettingRepository, never()).save(any(SystemSetting.class));
    }

    @Test
    void getLoginRateLimitSettingsValues_isReadableWithoutSuperAdminRole() {
        stubLoginRows("true", "7", "20");

        LoginRateLimitSettings values = systemSettingService.getLoginRateLimitSettingsValues();

        assertThat(values.enabled()).isTrue();
        assertThat(values.maxFailedAttempts()).isEqualTo(7);
        assertThat(values.lockoutDurationMinutes()).isEqualTo(20);
        verify(securityUtils, never()).hasRole(anyString());
    }

    @Test
    void getLoginRateLimitSettingsValues_fallsBackToDefaults_whenRepositoryFails() {
        when(systemSettingRepository.findBySettingKey(anyString()))
                .thenThrow(new RuntimeException("db down"));

        LoginRateLimitSettings values = systemSettingService.getLoginRateLimitSettingsValues();

        assertThat(values.enabled()).isTrue();
        assertThat(values.maxFailedAttempts()).isEqualTo(5);
        assertThat(values.lockoutDurationMinutes()).isEqualTo(15);
    }

    @Test
    void settingsAreReadFreshOnEveryCallSoChangesApplyImmediately() {
        stubLoginRows("true", "5", "15");
        assertThat(systemSettingService.getLoginRateLimitSettingsValues().maxFailedAttempts()).isEqualTo(5);

        stubLoginRows("true", "2", "60");
        assertThat(systemSettingService.getLoginRateLimitSettingsValues().maxFailedAttempts()).isEqualTo(2);
        assertThat(systemSettingService.getLoginRateLimitSettingsValues().lockoutDurationMinutes()).isEqualTo(60);
    }

    private Map<String, String> captureSavedLoginValues() {
        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository, atLeastOnce()).save(captor.capture());
        Map<String, String> saved = new HashMap<>();
        for (SystemSetting setting : captor.getAllValues()) {
            if (setting.getSettingKey() != null && setting.getSettingKey().startsWith("LOGIN_")) {
                saved.put(setting.getSettingKey(), setting.getSettingValue());
            }
        }
        return saved;
    }

    // --- Super admin Telegram destination -------------------------------------

    private static final String TG_TOKEN = "123456789:AAFakeTokenForTestsOnly_doNotUse";
    private static final String TG_MASK = "***************";

    /** Stubs the three Telegram rows from a key->value map; absent keys do not exist. */
    private void stubTelegramRows(Map<String, String> rows) {
        when(systemSettingRepository.findBySettingKey(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String value = rows.get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .updatedAt(LocalDateTime.now())
                    .build());
        });
    }

    private Map<String, String> configuredTelegramRows() {
        Map<String, String> rows = new HashMap<>();
        rows.put(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_ENABLED, "true");
        rows.put(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN, TG_TOKEN);
        rows.put(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID, "-1001234567890");
        return rows;
    }

    @Test
    void getSuperAdminTelegramSettings_masksStoredToken() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(configuredTelegramRows());

        SuperAdminTelegramSettingsResponse response = systemSettingService.getSuperAdminTelegramSettings();

        assertThat(response.enabled()).isTrue();
        assertThat(response.botTokenConfigured()).isTrue();
        assertThat(response.maskedBotToken()).isEqualTo(TG_MASK);
        assertThat(response.chatId()).isEqualTo("-1001234567890");
    }

    @Test
    void getSuperAdminTelegramSettings_neverReturnsTheRealToken() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(configuredTelegramRows());

        SuperAdminTelegramSettingsResponse response = systemSettingService.getSuperAdminTelegramSettings();

        assertThat(response.maskedBotToken()).isNotEqualTo(TG_TOKEN);
        assertThat(response.toString()).doesNotContain(TG_TOKEN);
    }

    @Test
    void getSuperAdminTelegramSettings_defaultsToDisabledWhenUnconfigured() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        SuperAdminTelegramSettingsResponse response = systemSettingService.getSuperAdminTelegramSettings();

        assertThat(response.enabled()).isFalse();
        assertThat(response.botTokenConfigured()).isFalse();
        assertThat(response.maskedBotToken()).isEmpty();
        assertThat(response.chatId()).isEmpty();
    }

    @Test
    void getSuperAdminTelegramSettings_throwsAccessDenied_whenNotSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemSettingService.getSuperAdminTelegramSettings())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSuperAdminTelegramSettings_throwsAccessDenied_whenNotSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, TG_TOKEN, "-1001234567890")))
                .isInstanceOf(AccessDeniedException.class);

        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSuperAdminTelegramSettings_persistsAllThreeValues() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, TG_TOKEN, "-1001234567890"));

        Map<String, String> saved = captureSavedValues("SUPER_ADMIN_TELEGRAM");
        assertThat(saved.get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_ENABLED)).isEqualTo("true");
        assertThat(saved.get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN)).isEqualTo(TG_TOKEN);
        assertThat(saved.get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID)).isEqualTo("-1001234567890");
    }

    @Test
    void updateSuperAdminTelegramSettings_returnsMaskedTokenAfterSave() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        SuperAdminTelegramSettingsResponse response = systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, TG_TOKEN, "-1001234567890"));

        assertThat(response.maskedBotToken()).isEqualTo(TG_MASK);
        assertThat(response.botTokenConfigured()).isTrue();
    }

    @Test
    void updateSuperAdminTelegramSettings_keepsStoredTokenWhenMaskIsSubmitted() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(configuredTelegramRows());

        systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, TG_MASK, "-1001234567890"));

        assertThat(captureSavedValues("SUPER_ADMIN_TELEGRAM")
                .get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN)).isEqualTo(TG_TOKEN);
    }

    @Test
    void updateSuperAdminTelegramSettings_keepsStoredTokenWhenBlankIsSubmitted() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(configuredTelegramRows());

        systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, "   ", "-1001234567890"));

        assertThat(captureSavedValues("SUPER_ADMIN_TELEGRAM")
                .get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN)).isEqualTo(TG_TOKEN);
    }

    @Test
    void updateSuperAdminTelegramSettings_rejectsMalformedToken() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        assertThatThrownBy(() -> systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, "not-a-bot-token", "-1001234567890")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("botToken");

        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSuperAdminTelegramSettings_rejectsMalformedChatId() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        assertThatThrownBy(() -> systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, TG_TOKEN, "chat id!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chatId");

        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSuperAdminTelegramSettings_rejectsEnablingWithoutCredentials() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        assertThatThrownBy(() -> systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSuperAdminTelegramSettings_allowsDisablingWithoutCredentials() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        stubTelegramRows(new HashMap<>());

        SuperAdminTelegramSettingsResponse response = systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(false, null, null));

        assertThat(response.enabled()).isFalse();
        assertThat(captureSavedValues("SUPER_ADMIN_TELEGRAM")
                .get(SystemSettingServiceImpl.KEY_SUPER_ADMIN_TELEGRAM_ENABLED)).isEqualTo("false");
    }

    @Test
    void updateSuperAdminTelegramSettings_requiresEnabledFlag() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> systemSettingService.updateSuperAdminTelegramSettings(
                new UpdateSuperAdminTelegramSettingsRequest(null, TG_TOKEN, "-1001234567890")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enabled");
    }

    @Test
    void getSuperAdminTelegramSettingsValues_doesNotRequireAnAuthenticatedRole() {
        stubTelegramRows(configuredTelegramRows());

        SuperAdminTelegramSettings values = systemSettingService.getSuperAdminTelegramSettingsValues();

        assertThat(values.enabled()).isTrue();
        assertThat(values.botToken()).isEqualTo(TG_TOKEN);
        assertThat(values.chatId()).isEqualTo("-1001234567890");
        assertThat(values.isDeliverable()).isTrue();
    }

    @Test
    void getSuperAdminTelegramSettingsValues_isNotDeliverableWhenDisabled() {
        assertThat(new SuperAdminTelegramSettings(false, TG_TOKEN, "-1001234567890").isDeliverable()).isFalse();
        assertThat(SuperAdminTelegramSettings.DISABLED.isDeliverable()).isFalse();
    }

    @Test
    void isTelegramNotificationEnabledFor_trueWhenEverySwitchIsOn() {
        stubNotificationRow("{\"systemNotificationsEnabled\":true,\"telegramNotificationsEnabled\":true,"
                + "\"propertySubmittedEnabled\":true}");

        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .isTrue();
    }

    @Test
    void isTelegramNotificationEnabledFor_falseWhenTelegramChannelIsOff() {
        stubNotificationRow("{\"systemNotificationsEnabled\":true,\"telegramNotificationsEnabled\":false,"
                + "\"propertySubmittedEnabled\":true}");

        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .isFalse();
    }

    @Test
    void isTelegramNotificationEnabledFor_falseWhenSystemNotificationsAreOff() {
        stubNotificationRow("{\"systemNotificationsEnabled\":false,\"telegramNotificationsEnabled\":true,"
                + "\"propertySubmittedEnabled\":true}");

        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .isFalse();
    }

    @Test
    void isTelegramNotificationEnabledFor_falseWhenPropertySubmittedIsOff() {
        stubNotificationRow("{\"systemNotificationsEnabled\":true,\"telegramNotificationsEnabled\":true,"
                + "\"propertySubmittedEnabled\":false}");

        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .isFalse();
    }

    @Test
    void isTelegramNotificationEnabledFor_falseForNullType() {
        assertThat(systemSettingService.isTelegramNotificationEnabledFor(null)).isFalse();
    }

    @Test
    void isTelegramNotificationEnabledFor_ignoresTheInAppChannelSwitch() {
        // In-app being off must not silently disable Telegram: the two channels
        // are independent apart from the shared system and per-event switches.
        stubNotificationRow("{\"systemNotificationsEnabled\":true,\"inAppNotificationsEnabled\":false,"
                + "\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":true}");

        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED))
                .isTrue();
    }

    @Test
    void isTelegramNotificationEnabledFor_sharesThePerEventSwitchWithInAppNotifications() {
        stubNotificationRow("{\"systemNotificationsEnabled\":true,\"inAppNotificationsEnabled\":true,"
                + "\"telegramNotificationsEnabled\":true,\"propertySubmittedEnabled\":false}");

        assertThat(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).isFalse();
        assertThat(systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED)).isFalse();
    }

    private void stubNotificationRow(String json) {
        when(systemSettingRepository.findBySettingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS))
                .thenReturn(Optional.of(SystemSetting.builder()
                        .settingKey(SystemSettingServiceImpl.KEY_SYSTEM_NOTIFICATION_SETTINGS)
                        .settingValue(json)
                        .build()));
    }

    private Map<String, String> captureSavedValues(String keyPrefix) {
        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository, atLeastOnce()).save(captor.capture());
        Map<String, String> saved = new HashMap<>();
        for (SystemSetting setting : captor.getAllValues()) {
            if (setting.getSettingKey() != null && setting.getSettingKey().startsWith(keyPrefix)) {
                saved.put(setting.getSettingKey(), setting.getSettingValue());
            }
        }
        return saved;
    }
}
