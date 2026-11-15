package com.example.spring_boot_project_api.service;

/**
 * Immutable snapshot of the SUPER_ADMIN Telegram destination, as persisted in
 * the existing {@code system_settings} table under the keys
 * {@code SUPER_ADMIN_TELEGRAM_ENABLED}, {@code SUPER_ADMIN_TELEGRAM_BOT_TOKEN}
 * and {@code SUPER_ADMIN_TELEGRAM_CHAT_ID}.
 *
 * <p>Internal value carrier used by the notification layer to deliver
 * {@code PROPERTY_SUBMITTED} messages to the super admin. It deliberately holds
 * the <em>real</em> bot token: it is never returned from a controller, so the
 * API surface can only expose a masked token.
 *
 * <p>Defaults are opt-in: disabled and unconfigured, so an installation that
 * never sets these keys performs no Telegram delivery at all.
 *
 * @param enabled  whether super admin notifications are delivered to Telegram
 * @param botToken bot token, blank when not configured
 * @param chatId   destination chat id, blank when not configured
 */
public record SuperAdminTelegramSettings(
        boolean enabled,
        String botToken,
        String chatId
) {

    public static final SuperAdminTelegramSettings DISABLED =
            new SuperAdminTelegramSettings(false, "", "");

    /** True only when the destination is enabled and has both credentials. */
    public boolean isDeliverable() {
        return enabled
                && botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }
}
