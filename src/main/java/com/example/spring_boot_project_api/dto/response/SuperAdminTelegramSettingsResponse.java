package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

/**
 * SUPER_ADMIN Telegram destination as seen by the API.
 *
 * <p>The bot token is never included: only whether one is stored and a fixed
 * mask, so no part of the credential can leak through the API.
 *
 * @param enabled            whether notifications are delivered to Telegram
 * @param botTokenConfigured whether a bot token is stored
 * @param maskedBotToken     fixed mask shown in place of the token
 * @param chatId             destination chat id (not a secret)
 * @param updatedAt          when the settings were last changed
 */
public record SuperAdminTelegramSettingsResponse(
        boolean enabled,
        boolean botTokenConfigured,
        String maskedBotToken,
        String chatId,
        LocalDateTime updatedAt
) {
}
