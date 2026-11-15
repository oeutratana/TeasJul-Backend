package com.example.spring_boot_project_api.enums;

/**
 * Purpose for which an OTP was generated. Each purpose uses a separate OTP
 * record so a code generated for one purpose can never be used for another
 * (e.g. a password-reset OTP can never be reused as a login 2FA code).
 */
public enum OtpPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    TWO_FACTOR_LOGIN,
    CHANGE_RECOVERY_EMAIL
}
