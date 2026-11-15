package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.UpdateLoginRateLimitSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSuperAdminTelegramSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemNotificationSettingsRequest;
import com.example.spring_boot_project_api.dto.response.LoginRateLimitSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SuperAdminTelegramSettingsResponse;
import com.example.spring_boot_project_api.dto.response.SystemNotificationSettingsResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.model.SystemSetting;
import com.example.spring_boot_project_api.repository.SystemSettingRepository;
import com.example.spring_boot_project_api.service.LoginRateLimitSettings;
import com.example.spring_boot_project_api.service.SuperAdminTelegramSettings;
import com.example.spring_boot_project_api.service.SystemSettingService;
import com.example.spring_boot_project_api.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String KEY_SYSTEM_NOTIFICATION_SETTINGS = "SYSTEM_NOTIFICATION_SETTINGS";

    /**
     * Login rate limiting is stored as three plain rows rather than one JSON
     * blob, so an operator can see each knob in {@code system_settings} next to
     * the key names used by this feature.
     */
    public static final String KEY_LOGIN_RATE_LIMIT_ENABLED = "LOGIN_RATE_LIMIT_ENABLED";
    public static final String KEY_LOGIN_MAX_FAILED_ATTEMPTS = "LOGIN_MAX_FAILED_ATTEMPTS";
    public static final String KEY_LOGIN_LOCKOUT_DURATION_MINUTES = "LOGIN_LOCKOUT_DURATION_MINUTES";

    /**
     * Defaults mirror the shipped {@code AUTH_LOGIN_ACCOUNT} policy
     * (5 attempts / 15m), so an unconfigured deployment keeps behaving exactly
     * as before this feature existed.
     */
    public static final boolean DEFAULT_LOGIN_RATE_LIMIT_ENABLED = true;
    public static final int DEFAULT_LOGIN_MAX_FAILED_ATTEMPTS = 5;
    public static final int DEFAULT_LOGIN_LOCKOUT_DURATION_MINUTES = 15;

    public static final int MIN_MAX_FAILED_ATTEMPTS = 1;
    public static final int MAX_MAX_FAILED_ATTEMPTS = 100;
    public static final int MIN_LOCKOUT_DURATION_MINUTES = 1;
    public static final int MAX_LOCKOUT_DURATION_MINUTES = 1440;

    /**
     * Destination for notifying the SUPER_ADMIN over Telegram (currently the
     * {@code PROPERTY_SUBMITTED} event). Stored as three plain rows in the same
     * table, mirroring the login rate-limit keys, so an operator can read and
     * edit each value in place.
     *
     * <p>These are super-admin scoped. The per-owner Telegram credentials keep
     * living in {@code owner_settings} and are untouched by these keys.</p>
     */
    public static final String KEY_SUPER_ADMIN_TELEGRAM_ENABLED = "SUPER_ADMIN_TELEGRAM_ENABLED";
    public static final String KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN = "SUPER_ADMIN_TELEGRAM_BOT_TOKEN";
    public static final String KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID = "SUPER_ADMIN_TELEGRAM_CHAT_ID";

    /** Opt-in: an installation that never configures these keys sends nothing. */
    public static final boolean DEFAULT_SUPER_ADMIN_TELEGRAM_ENABLED = false;

    /** Fixed mask returned instead of the bot token; echoing it back keeps the stored token. */
    public static final String SUPER_ADMIN_TELEGRAM_TOKEN_MASK = "***************";

    /**
     * Telegram chat targets: a numeric chat id (channel ids are negative) or an
     * {@code @channelusername}. Kept in sync with what the Telegram Bot API
     * accepts for {@code chat_id}.
     */
    private static final Pattern TELEGRAM_CHAT_ID = Pattern.compile("^(-?\\d{1,20}|@[A-Za-z0-9_]{5,32})$");

    /**
     * Telegram bot tokens are {@code <bot_id>:<secret>}. Validated leniently so a
     * typo is rejected early without locking out a legitimate token.
     */
    private static final Pattern TELEGRAM_BOT_TOKEN = Pattern.compile("^\\d{5,12}:[A-Za-z0-9_-]{20,}$");

    private final SystemSettingRepository systemSettingRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public SystemNotificationSettingsResponse getNotificationSettings() {
        requireSuperAdmin();
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .orElseGet(this::createDefaultNotificationSetting);
        return toNotificationResponse(setting);
    }

    @Override
    @Transactional
    public SystemNotificationSettingsResponse updateNotificationSettings(UpdateSystemNotificationSettingsRequest request) {
        requireSuperAdmin();
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                .orElseGet(this::createDefaultNotificationSetting);

        try {
            JsonNode existingRoot = OBJECT_MAPPER.readTree(setting.getSettingValue());
            ObjectNode root = existingRoot.isObject() ? (ObjectNode) existingRoot : OBJECT_MAPPER.createObjectNode();

            if (request.systemNotificationsEnabled() != null) {
                root.put("systemNotificationsEnabled", request.systemNotificationsEnabled());
            }
            if (request.inAppNotificationsEnabled() != null) {
                root.put("inAppNotificationsEnabled", request.inAppNotificationsEnabled());
            }
            if (request.telegramNotificationsEnabled() != null) {
                root.put("telegramNotificationsEnabled", request.telegramNotificationsEnabled());
            }
            if (request.propertySubmittedEnabled() != null) {
                root.put("propertySubmittedEnabled", request.propertySubmittedEnabled());
            }
            if (request.invoiceCreatedEnabled() != null) {
                root.put("invoiceCreatedEnabled", request.invoiceCreatedEnabled());
            }
            if (request.paymentCreatedEnabled() != null) {
                root.put("paymentCreatedEnabled", request.paymentCreatedEnabled());
            }
            if (request.paymentSuccessEnabled() != null) {
                root.put("paymentSuccessEnabled", request.paymentSuccessEnabled());
            }
            if (request.paymentFailedEnabled() != null) {
                root.put("paymentFailedEnabled", request.paymentFailedEnabled());
            }
            if (request.maintenanceRequestEnabled() != null) {
                root.put("maintenanceRequestEnabled", request.maintenanceRequestEnabled());
            }
            if (request.maintenanceStatusEnabled() != null) {
                root.put("maintenanceStatusEnabled", request.maintenanceStatusEnabled());
            }
            if (request.contractExpiringEnabled() != null) {
                root.put("contractExpiringEnabled", request.contractExpiringEnabled());
            }
            if (request.roomAssignedEnabled() != null) {
                root.put("roomAssignedEnabled", request.roomAssignedEnabled());
            }

            setting.setSettingValue(OBJECT_MAPPER.writeValueAsString(root));
            SystemSetting saved = systemSettingRepository.save(setting);
            return toNotificationResponse(saved);
        } catch (Exception e) {
            log.error("Failed to update system notification settings", e);
            throw new RuntimeException("Failed to update system notification settings", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationTypeEnabled(NotificationType type) {
        if (type == null) {
            return false;
        }
        try {
            SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                    .orElse(null);
            if (setting == null) {
                return true;
            }
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettingValue());
            boolean systemEnabled = getBooleanOrDefault(root, "systemNotificationsEnabled", true);
            boolean inAppEnabled = getBooleanOrDefault(root, "inAppNotificationsEnabled", true);
            if (!systemEnabled || !inAppEnabled) {
                return false;
            }
            return isEventEnabled(root, type);
        } catch (Exception e) {
            log.warn("Failed to check notification setting for type: {}, defaulting to true", type, e);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTelegramNotificationEnabled() {
        try {
            SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                    .orElse(null);
            if (setting == null) {
                return true;
            }
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettingValue());
            boolean systemEnabled = getBooleanOrDefault(root, "systemNotificationsEnabled", true);
            boolean telegramEnabled = getBooleanOrDefault(root, "telegramNotificationsEnabled", true);
            return systemEnabled && telegramEnabled;
        } catch (Exception e) {
            log.warn("Failed to check telegram notification setting, defaulting to true", e);
            return true;
        }
    }

    /**
     * Telegram delivery gate for one event: the system switch, the Telegram
     * channel and the per-event flag must all be on. Shares the per-event switch
     * with {@link #isNotificationTypeEnabled(NotificationType)} so both channels
     * always agree on which events are enabled.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTelegramNotificationEnabledFor(NotificationType type) {
        if (type == null) {
            return false;
        }
        try {
            SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                    .orElse(null);
            if (setting == null) {
                return true;
            }
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettingValue());
            boolean systemEnabled = getBooleanOrDefault(root, "systemNotificationsEnabled", true);
            boolean telegramEnabled = getBooleanOrDefault(root, "telegramNotificationsEnabled", true);
            if (!systemEnabled || !telegramEnabled) {
                return false;
            }
            return isEventEnabled(root, type);
        } catch (Exception e) {
            log.warn("Failed to check telegram notification setting for type: {}, defaulting to true", type, e);
            return true;
        }
    }

    /** Per-event flag, defaulting to enabled when absent. */
    private boolean isEventEnabled(JsonNode root, NotificationType type) {
        return switch (type) {
            case PROPERTY_SUBMITTED -> getBooleanOrDefault(root, "propertySubmittedEnabled", true);
            case INVOICE_CREATED -> getBooleanOrDefault(root, "invoiceCreatedEnabled", true);
            case PAYMENT_CREATED -> getBooleanOrDefault(root, "paymentCreatedEnabled", true);
            case PAYMENT_SUCCESS -> getBooleanOrDefault(root, "paymentSuccessEnabled", true);
            case PAYMENT_FAILED -> getBooleanOrDefault(root, "paymentFailedEnabled", true);
            case MAINTENANCE_REQUEST -> getBooleanOrDefault(root, "maintenanceRequestEnabled", true);
            case MAINTENANCE_STATUS -> getBooleanOrDefault(root, "maintenanceStatusEnabled", true);
            case CONTRACT_EXPIRING -> getBooleanOrDefault(root, "contractExpiringEnabled", true);
            case ROOM_ASSIGNED -> getBooleanOrDefault(root, "roomAssignedEnabled", true);
        };
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("Only SUPER_ADMIN can manage system notification settings");
        }
    }

    private void requireSuperAdminForLoginRateLimit() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("Only SUPER_ADMIN can manage login rate limit settings");
        }
    }

    private void requireSuperAdminForSuperAdminTelegram() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("Only SUPER_ADMIN can manage the super admin Telegram settings");
        }
    }

    /**
     * Reads the three super admin Telegram rows leniently: any missing or
     * unparseable value falls back to the safe default (disabled, no
     * credentials), so a partially written table can never start delivering.
     */
    private SuperAdminTelegramSettings readSuperAdminTelegramSettings() {
        return new SuperAdminTelegramSettings(
                parseBoolean(readSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_ENABLED),
                        DEFAULT_SUPER_ADMIN_TELEGRAM_ENABLED),
                trimToEmpty(readSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN)),
                trimToEmpty(readSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID))
        );
    }

    private String readSuperAdminTelegramSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    private LocalDateTime lastSuperAdminTelegramUpdate() {
        LocalDateTime latest = null;
        for (String key : List.of(KEY_SUPER_ADMIN_TELEGRAM_ENABLED, KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN,
                KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID)) {
            LocalDateTime updatedAt = systemSettingRepository.findBySettingKey(key)
                    .map(SystemSetting::getUpdatedAt)
                    .orElse(null);
            if (updatedAt != null && (latest == null || updatedAt.isAfter(latest))) {
                latest = updatedAt;
            }
        }
        return latest;
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminTelegramSettingsResponse getSuperAdminTelegramSettings() {
        requireSuperAdminForSuperAdminTelegram();
        return toSuperAdminTelegramResponse(readSuperAdminTelegramSettings());
    }

    /**
     * The bot token is write-only: a blank value or the mask returned by the API
     * keeps the stored token, so the UI can save the form without ever receiving
     * the real credential.
     */
    @Override
    @Transactional
    public SuperAdminTelegramSettingsResponse updateSuperAdminTelegramSettings(
            UpdateSuperAdminTelegramSettingsRequest request) {
        requireSuperAdminForSuperAdminTelegram();
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.enabled() == null) {
            throw new IllegalArgumentException("enabled is required");
        }
        boolean enabled = request.enabled();

        SuperAdminTelegramSettings current = readSuperAdminTelegramSettings();
        String botToken = resolveIncomingBotToken(request.botToken(), current.botToken());
        String chatId = resolveIncomingChatId(request.chatId(), current.chatId());

        if (enabled && (botToken.isEmpty() || chatId.isEmpty())) {
            throw new IllegalArgumentException(
                    "Bot token and chat ID are required before Telegram notifications can be enabled");
        }

        writeSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_ENABLED, Boolean.toString(enabled),
                "Whether PROPERTY_SUBMITTED notifications are delivered to the SUPER_ADMIN over Telegram");
        writeSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_BOT_TOKEN, botToken,
                "Telegram bot token used to notify the SUPER_ADMIN");
        writeSuperAdminTelegramSetting(KEY_SUPER_ADMIN_TELEGRAM_CHAT_ID, chatId,
                "Telegram chat id that receives SUPER_ADMIN notifications");

        return toSuperAdminTelegramResponse(new SuperAdminTelegramSettings(enabled, botToken, chatId));
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminTelegramSettings getSuperAdminTelegramSettingsValues() {
        try {
            return readSuperAdminTelegramSettings();
        } catch (Exception e) {
            log.warn("Failed to read super admin Telegram settings, falling back to disabled", e);
            return SuperAdminTelegramSettings.DISABLED;
        }
    }

    private String resolveIncomingBotToken(String incoming, String stored) {
        if (incoming == null) {
            return stored;
        }
        String candidate = incoming.trim();
        // Unchanged: blank or the mask means "keep what is stored".
        if (candidate.isEmpty() || SUPER_ADMIN_TELEGRAM_TOKEN_MASK.equals(candidate)) {
            return stored;
        }
        if (!TELEGRAM_BOT_TOKEN.matcher(candidate).matches()) {
            throw new IllegalArgumentException("botToken must look like 123456789:AA...");
        }
        return candidate;
    }

    private String resolveIncomingChatId(String incoming, String stored) {
        if (incoming == null) {
            return stored;
        }
        String candidate = incoming.trim();
        if (candidate.isEmpty()) {
            return stored;
        }
        if (!TELEGRAM_CHAT_ID.matcher(candidate).matches()) {
            throw new IllegalArgumentException(
                    "chatId must be a numeric Telegram chat id (e.g. -1001234567890) or @channelusername");
        }
        return candidate;
    }

    private void writeSuperAdminTelegramSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        setting.setDescription(description);
        systemSettingRepository.save(setting);
    }

    /** Masks the token: the API never returns the credential itself. */
    private SuperAdminTelegramSettingsResponse toSuperAdminTelegramResponse(
            SuperAdminTelegramSettings settings) {
        boolean tokenConfigured = settings.botToken() != null && !settings.botToken().isBlank();
        return new SuperAdminTelegramSettingsResponse(
                settings.enabled(),
                tokenConfigured,
                tokenConfigured ? SUPER_ADMIN_TELEGRAM_TOKEN_MASK : "",
                settings.chatId() == null ? "" : settings.chatId(),
                lastSuperAdminTelegramUpdate()
        );
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginRateLimitSettingsResponse getLoginRateLimitSettings() {
        requireSuperAdminForLoginRateLimit();
        LoginRateLimitSettings settings = readLoginRateLimitSettings();
        return new LoginRateLimitSettingsResponse(
                settings.enabled(),
                settings.maxFailedAttempts(),
                settings.lockoutDurationMinutes(),
                lastLoginRateLimitUpdate()
        );
    }

    @Override
    @Transactional
    public LoginRateLimitSettingsResponse updateLoginRateLimitSettings(
            UpdateLoginRateLimitSettingsRequest request) {
        requireSuperAdminForLoginRateLimit();
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        boolean enabled = requireLoginRateLimitEnabled(request.loginRateLimitEnabled());
        int maxFailedAttempts = requireMaxFailedAttempts(request.maxFailedAttempts());
        int lockoutDurationMinutes = requireLockoutDurationMinutes(request.lockoutDurationMinutes());

        writeLoginRateLimitSetting(KEY_LOGIN_RATE_LIMIT_ENABLED, Boolean.toString(enabled),
                "Whether login rate limiting is enforced for the login endpoint");
        writeLoginRateLimitSetting(KEY_LOGIN_MAX_FAILED_ATTEMPTS, Integer.toString(maxFailedAttempts),
                "Maximum failed login attempts allowed per account inside the lockout window");
        writeLoginRateLimitSetting(KEY_LOGIN_LOCKOUT_DURATION_MINUTES, Integer.toString(lockoutDurationMinutes),
                "How long a login account stays locked out after reaching the attempt limit");

        return new LoginRateLimitSettingsResponse(enabled, maxFailedAttempts, lockoutDurationMinutes,
                lastLoginRateLimitUpdate());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginRateLimitSettings getLoginRateLimitSettingsValues() {
        try {
            return readLoginRateLimitSettings();
        } catch (Exception e) {
            log.warn("Failed to read login rate limit settings, falling back to defaults", e);
            return defaultLoginRateLimitSettings();
        }
    }

    /**
     * Reads the three login rows leniently: any missing or unparseable value
     * falls back to the shipped default, so a partially written or hand-edited
     * table can never disable protection or break the login endpoint.
     */
    private LoginRateLimitSettings readLoginRateLimitSettings() {
        return new LoginRateLimitSettings(
                parseBoolean(readLoginRateLimitSetting(KEY_LOGIN_RATE_LIMIT_ENABLED),
                        DEFAULT_LOGIN_RATE_LIMIT_ENABLED),
                parseBoundedInt(readLoginRateLimitSetting(KEY_LOGIN_MAX_FAILED_ATTEMPTS),
                        DEFAULT_LOGIN_MAX_FAILED_ATTEMPTS, MIN_MAX_FAILED_ATTEMPTS, MAX_MAX_FAILED_ATTEMPTS),
                parseBoundedInt(readLoginRateLimitSetting(KEY_LOGIN_LOCKOUT_DURATION_MINUTES),
                        DEFAULT_LOGIN_LOCKOUT_DURATION_MINUTES, MIN_LOCKOUT_DURATION_MINUTES,
                        MAX_LOCKOUT_DURATION_MINUTES)
        );
    }

    private String readLoginRateLimitSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    private LocalDateTime lastLoginRateLimitUpdate() {
        LocalDateTime latest = null;
        for (String key : List.of(KEY_LOGIN_RATE_LIMIT_ENABLED, KEY_LOGIN_MAX_FAILED_ATTEMPTS,
                KEY_LOGIN_LOCKOUT_DURATION_MINUTES)) {
            LocalDateTime updatedAt = systemSettingRepository.findBySettingKey(key)
                    .map(SystemSetting::getUpdatedAt)
                    .orElse(null);
            if (updatedAt != null && (latest == null || updatedAt.isAfter(latest))) {
                latest = updatedAt;
            }
        }
        return latest;
    }

    private void writeLoginRateLimitSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        setting.setDescription(description);
        systemSettingRepository.save(setting);
    }

    private boolean requireLoginRateLimitEnabled(Boolean value) {
        if (value == null) {
            throw new IllegalArgumentException("loginRateLimitEnabled is required");
        }
        return value;
    }

    private int requireMaxFailedAttempts(Number value) {
        return requireInRange(value, "maxFailedAttempts", MIN_MAX_FAILED_ATTEMPTS, MAX_MAX_FAILED_ATTEMPTS);
    }

    private int requireLockoutDurationMinutes(Number value) {
        return requireInRange(value, "lockoutDurationMinutes", MIN_LOCKOUT_DURATION_MINUTES,
                MAX_LOCKOUT_DURATION_MINUTES);
    }

    private int requireInRange(Number value, String field, int min, int max) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        double asDouble = value.doubleValue();
        if (Double.isNaN(asDouble) || Double.isInfinite(asDouble) || asDouble != Math.rint(asDouble)) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
        long asLong = value.longValue();
        if (asLong < min || asLong > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
        return (int) asLong;
    }

    private boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        String trimmed = raw.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        return defaultValue;
    }

    private int parseBoundedInt(String raw, int defaultValue, int min, int max) {
        if (raw == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed < min || parsed > max ? defaultValue : parsed;
        } catch (NumberFormatException notANumber) {
            return defaultValue;
        }
    }

    private LoginRateLimitSettings defaultLoginRateLimitSettings() {
        return new LoginRateLimitSettings(
                DEFAULT_LOGIN_RATE_LIMIT_ENABLED,
                DEFAULT_LOGIN_MAX_FAILED_ATTEMPTS,
                DEFAULT_LOGIN_LOCKOUT_DURATION_MINUTES
        );
    }

    private SystemSetting createDefaultNotificationSetting() {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();
            root.put("systemNotificationsEnabled", true);
            root.put("inAppNotificationsEnabled", true);
            root.put("telegramNotificationsEnabled", true);
            root.put("propertySubmittedEnabled", true);
            root.put("invoiceCreatedEnabled", true);
            root.put("paymentCreatedEnabled", true);
            root.put("paymentSuccessEnabled", true);
            root.put("paymentFailedEnabled", true);
            root.put("maintenanceRequestEnabled", true);
            root.put("maintenanceStatusEnabled", true);
            root.put("contractExpiringEnabled", true);
            root.put("roomAssignedEnabled", true);

            SystemSetting setting = SystemSetting.builder()
                    .settingKey(KEY_SYSTEM_NOTIFICATION_SETTINGS)
                    .settingValue(OBJECT_MAPPER.writeValueAsString(root))
                    .description("System-wide notification preferences")
                    .build();
            return systemSettingRepository.save(setting);
        } catch (Exception e) {
            log.error("Failed to create default notification settings", e);
            throw new RuntimeException("Failed to initialize notification settings", e);
        }
    }

    private SystemNotificationSettingsResponse toNotificationResponse(SystemSetting setting) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettingValue());
            return new SystemNotificationSettingsResponse(
                    getBooleanOrDefault(root, "systemNotificationsEnabled", true),
                    getBooleanOrDefault(root, "inAppNotificationsEnabled", true),
                    getBooleanOrDefault(root, "telegramNotificationsEnabled", true),
                    getBooleanOrDefault(root, "propertySubmittedEnabled", true),
                    getBooleanOrDefault(root, "invoiceCreatedEnabled", true),
                    getBooleanOrDefault(root, "paymentCreatedEnabled", true),
                    getBooleanOrDefault(root, "paymentSuccessEnabled", true),
                    getBooleanOrDefault(root, "paymentFailedEnabled", true),
                    getBooleanOrDefault(root, "maintenanceRequestEnabled", true),
                    getBooleanOrDefault(root, "maintenanceStatusEnabled", true),
                    getBooleanOrDefault(root, "contractExpiringEnabled", true),
                    getBooleanOrDefault(root, "roomAssignedEnabled", true),
                    setting.getUpdatedAt() != null ? setting.getUpdatedAt() : LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Failed to parse notification settings", e);
            throw new RuntimeException("Failed to parse notification settings", e);
        }
    }

    private boolean getBooleanOrDefault(JsonNode node, String field, boolean defaultValue) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asBoolean(defaultValue);
        }
        return defaultValue;
    }
}
