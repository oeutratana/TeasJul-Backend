package com.example.spring_boot_project_api.ratelimit;

import com.example.spring_boot_project_api.service.LoginRateLimitSettings;
import com.example.spring_boot_project_api.service.SystemSettingService;
import com.example.spring_boot_project_api.service.impl.SystemSettingServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Reads the login rate-limiting settings from the existing
 * {@code system_settings} table through {@link SystemSettingService}.
 *
 * <p>{@link SystemSettingServiceImpl#getLoginRateLimitSettingsValues()} is the
 * deliberately unguarded read: it is needed on the login request path, where no
 * SUPER_ADMIN principal exists yet, and it already degrades to the shipped
 * defaults on any failure so the limiter always has usable numbers. Writes stay
 * behind the SUPER_ADMIN check in
 * {@link SystemSettingServiceImpl#updateLoginRateLimitSettings}.
 *
 * <p>Registered as a bean in {@link RateLimitFilterRegistration} rather than
 * annotated {@code @Component}, keeping every rate-limiting bean out of
 * {@code @WebMvcTest} slices like the rest of this package.
 */
@RequiredArgsConstructor
public class SystemSettingLoginRateLimitSettingsSource implements LoginRateLimitSettingsSource {

    private final SystemSettingService systemSettingService;

    @Override
    public LoginRateLimitSettings current() {
        LoginRateLimitSettings settings = systemSettingService.getLoginRateLimitSettingsValues();
        return settings == null ? new LoginRateLimitSettings(
                SystemSettingServiceImpl.DEFAULT_LOGIN_RATE_LIMIT_ENABLED,
                SystemSettingServiceImpl.DEFAULT_LOGIN_MAX_FAILED_ATTEMPTS,
                SystemSettingServiceImpl.DEFAULT_LOGIN_LOCKOUT_DURATION_MINUTES
        ) : settings;
    }
}
