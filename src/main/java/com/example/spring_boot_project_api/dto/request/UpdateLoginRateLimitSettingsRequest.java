package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * SUPER_ADMIN request body for the login rate-limiting settings.
 *
 * <p>All three values are required: the screen saves the whole set at once, so a
 * partial update is rejected instead of silently keeping stale numbers.
 *
 * <p>The numeric fields are declared as {@link Number} and narrowed by the
 * compact constructor. Binding straight to {@code Integer} would let Jackson
 * coerce {@code 5.7} to {@code 5}, quietly storing a different number than the
 * one that was submitted; rejecting the fractional payload here turns it into a
 * 400 instead. {@code NaN} and infinities are covered by the same check.
 */
public record UpdateLoginRateLimitSettingsRequest(

        @NotNull(message = "loginRateLimitEnabled is required")
        Boolean loginRateLimitEnabled,

        @NotNull(message = "maxFailedAttempts is required")
        @Min(value = 1, message = "maxFailedAttempts must be at least 1")
        @Max(value = 100, message = "maxFailedAttempts must be at most 100")
        Number maxFailedAttempts,

        @NotNull(message = "lockoutDurationMinutes is required")
        @Min(value = 1, message = "lockoutDurationMinutes must be at least 1")
        @Max(value = 1440, message = "lockoutDurationMinutes must be at most 1440")
        Number lockoutDurationMinutes
) {

    public UpdateLoginRateLimitSettingsRequest {
        maxFailedAttempts = requireWholeNumber(maxFailedAttempts, "maxFailedAttempts");
        lockoutDurationMinutes = requireWholeNumber(lockoutDurationMinutes, "lockoutDurationMinutes");
    }

    /**
     * @return {@code value} as an {@link Integer} when it is a real whole number
     * @throws IllegalArgumentException when it is fractional, NaN or infinite
     */
    private static Integer requireWholeNumber(Number value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long
                || value instanceof java.math.BigInteger || value instanceof Short
                || value instanceof Byte) {
            long asLong = value.longValue();
            if (asLong < Integer.MIN_VALUE || asLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(field + " is out of range");
            }
            return (int) asLong;
        }
        double asDouble = value.doubleValue();
        if (Double.isNaN(asDouble) || Double.isInfinite(asDouble) || asDouble != Math.rint(asDouble)) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
        return (int) asDouble;
    }
}
