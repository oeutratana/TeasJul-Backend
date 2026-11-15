package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

/**
 * Current login rate-limiting settings as enforced by the existing limiter.
 *
 * @param loginRateLimitEnabled    whether login rate limiting is enforced
 * @param maxFailedAttempts        per-account attempts allowed inside the lockout window
 * @param lockoutDurationMinutes   length of the lockout window in minutes
 * @param updatedAt                when the underlying rows were last written
 */
public record LoginRateLimitSettingsResponse(
        boolean loginRateLimitEnabled,
        int maxFailedAttempts,
        int lockoutDurationMinutes,
        LocalDateTime updatedAt
) {
}
