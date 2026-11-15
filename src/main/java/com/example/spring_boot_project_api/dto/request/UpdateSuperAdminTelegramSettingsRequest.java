package com.example.spring_boot_project_api.dto.request;

/**
 * SUPER_ADMIN Telegram destination update.
 *
 * <p>The bot token is write-only. Leaving {@code botToken} blank - or echoing
 * back the mask returned by the API - keeps the stored token untouched, so the
 * UI never has to receive the real credential to leave it alone.
 *
 * @param enabled whether PROPERTY_SUBMITTED notifications are delivered to Telegram
 * @param botToken bot token; blank or the mask keeps the stored token
 * @param chatId  destination chat id, numeric (optionally negative) or {@code @channelusername}
 */
public record UpdateSuperAdminTelegramSettingsRequest(
        Boolean enabled,
        String botToken,
        String chatId
) {
}
