package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.UpdateLoginRateLimitSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSuperAdminTelegramSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemNotificationSettingsRequest;
import com.example.spring_boot_project_api.dto.response.LoginRateLimitSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SuperAdminTelegramSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SystemNotificationSettingsResponse;
import com.example.spring_boot_project_api.enums.NotificationType;

public interface SystemSettingService {

    SystemNotificationSettingsResponse getNotificationSettings();

    SystemNotificationSettingsResponse updateNotificationSettings(UpdateSystemNotificationSettingsRequest request);

    boolean isNotificationTypeEnabled(NotificationType type);

    boolean isTelegramNotificationEnabled();

    /**
     * Telegram counterpart of {@link #isNotificationTypeEnabled(NotificationType)}:
     * true only when system notifications, the Telegram channel and the event
     * itself are all enabled.
     */
    boolean isTelegramNotificationEnabledFor(NotificationType type);

    LoginRateLimitSettingsResponse getLoginRateLimitSettings();

    LoginRateLimitSettingsResponse updateLoginRateLimitSettings(UpdateLoginRateLimitSettingsRequest request);

    LoginRateLimitSettings getLoginRateLimitSettingsValues();

    SuperAdminTelegramSettingsResponse getSuperAdminTelegramSettings();

    SuperAdminTelegramSettingsResponse updateSuperAdminTelegramSettings(
            UpdateSuperAdminTelegramSettingsRequest request);

    /** Internal read used by the notification layer; never exposed by a controller. */
    SuperAdminTelegramSettings getSuperAdminTelegramSettingsValues();
}
