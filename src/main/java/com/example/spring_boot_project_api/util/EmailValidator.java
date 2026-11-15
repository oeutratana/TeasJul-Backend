package com.example.spring_boot_project_api.util;

import java.util.Locale;

/**
 * Role-aware email validation for the system.
 *
 * <p>The system distinguishes four distinct email concepts that must never be
 * conflated:
 * <ol>
 *   <li><b>Internal login email</b> — internal users (every non-TENANT role such
 *       as OWNER/MANAGER/ADMIN/STAFF) must log in with an address on the
 *       corporate domain (e.g. {@code manager@rental.com}).</li>
 *   <li><b>Recovery email</b> — a separate real, externally reachable inbox
 *       (e.g. a Gmail address) used by internal users for password recovery and
 *       email 2FA. It must be verified before use.</li>
 *   <li><b>Tenant email</b> — the tenant's primary email, used for invitation,
 *       verification, login, notifications and forgot-password. Tenants may use
 *       any valid external email (Gmail, Outlook, Yahoo, university domains, ...)
 *       and are <b>not</b> bound to the corporate domain, nor do they require a
 *       separate recovery email.</li>
 *   <li><b>SMTP sender</b> — a real configured sender account, unrelated to the
 *       above and supplied via environment configuration only.</li>
 * </ol>
 *
 * <p>The {@code @rental.com} domain rule applies <b>only</b> to internal
 * (non-TENANT) accounts. Tenant login emails may use any valid domain.
 */
public final class EmailValidator {

    /** Corporate domain required for internal (non-TENANT) login emails. */
    public static final String INTERNAL_DOMAIN = "rental.com";

    /** The single role that is an external customer and exempt from the domain rule. */
    public static final String TENANT_ROLE = "TENANT";

    private EmailValidator() {
    }

    /**
     * Whether the given role code represents an internal system user (i.e. any
     * role that is <em>not</em> a tenant). This keeps the rule open to future
     * internal roles (ADMIN, STAFF, ...) without additional changes.
     */
    public static boolean isInternalRole(String roleCode) {
        return roleCode != null && !TENANT_ROLE.equalsIgnoreCase(roleCode.trim());
    }

    /**
     * True when the email belongs to the corporate internal domain
     * (case-insensitive), e.g. {@code manager@rental.com}.
     */
    public static boolean isOnInternalDomain(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at < 0 || at == trimmed.length() - 1) {
            return false;
        }
        String domain = trimmed.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        return INTERNAL_DOMAIN.equals(domain);
    }

    /**
     * Returns a human-readable requirement message for internal users. Tenants
     * are explicitly exempt from this rule.
     */
    public static String internalEmailRequirement() {
        return "Internal user email must end with @" + INTERNAL_DOMAIN
                + " (e.g. user@" + INTERNAL_DOMAIN + ").";
    }
}