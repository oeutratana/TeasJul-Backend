package com.example.spring_boot_project_api.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UpdateLoginRateLimitSettingsRequestTest {

    @Test
    void acceptsWholeNumbersFromAnyJsonNumericRepresentation() {
        assertThat(request(5L, 15L).maxFailedAttempts()).isEqualTo(5);
        assertThat(request(5.0d, 15.0d).lockoutDurationMinutes()).isEqualTo(15);
        assertThat(request((short) 5, (byte) 15).maxFailedAttempts()).isEqualTo(5);
    }

    @Test
    void rejectsFractionalValues() {
        assertThatThrownBy(() -> request(5.7d, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFailedAttempts must be a whole number");
        assertThatThrownBy(() -> request(5, 0.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockoutDurationMinutes must be a whole number");
    }

    @Test
    void rejectsNaNAndInfinities() {
        assertThatThrownBy(() -> request(Double.NaN, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFailedAttempts must be a whole number");
        assertThatThrownBy(() -> request(5, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockoutDurationMinutes must be a whole number");
        assertThatThrownBy(() -> request(5, Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockoutDurationMinutes must be a whole number");
    }

    @Test
    void keepsNullsSoNotNullValidationCanReportThem() {
        UpdateLoginRateLimitSettingsRequest request = new UpdateLoginRateLimitSettingsRequest(null, null, null);

        assertThat(request.loginRateLimitEnabled()).isNull();
        assertThat(request.maxFailedAttempts()).isNull();
        assertThat(request.lockoutDurationMinutes()).isNull();
    }

    private static UpdateLoginRateLimitSettingsRequest request(Number attempts, Number lockout) {
        return new UpdateLoginRateLimitSettingsRequest(true, attempts, lockout);
    }
}
