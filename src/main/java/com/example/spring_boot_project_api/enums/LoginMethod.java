package com.example.spring_boot_project_api.enums;

/**
 * The authentication path a sign-in attempt actually completed, reflecting the
 * existing flow in {@code AuthServiceImpl}:
 *
 * <ul>
 *   <li>{@link #PASSWORD} - password verified and a JWT issued directly.</li>
 *   <li>{@link #PASSWORD_2FA} - password verified, then the emailed two-factor
 *       challenge was completed before the JWT was issued.</li>
 * </ul>
 */
public enum LoginMethod {
    PASSWORD,
    PASSWORD_2FA
}
