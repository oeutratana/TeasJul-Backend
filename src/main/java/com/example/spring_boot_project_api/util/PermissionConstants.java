package com.example.spring_boot_project_api.util;

/**
 * Single source of truth for permission codes.
 *
 * <p>Permissions follow the {@code MODULE_ACTION} convention. Codes declared here
 * are also seeded (idempotently) into the {@code permissions} table by the
 * {@code V46__seed_permission_catalog.sql} Flyway migration and assigned to the
 * system roles. Enforcement is done through {@link SecurityUtils#hasPermission(String)}
 * / {@link SecurityUtils#requirePermission(String)}, which always grant the OWNER role
 * full access regardless of stored role-permission rows.</p>
 */
public final class PermissionConstants {

    private PermissionConstants() {
    }

    // ---- HOUSE ----
    public static final String HOUSE_VIEW = "HOUSE_VIEW";
    public static final String HOUSE_CREATE = "HOUSE_CREATE";
    public static final String HOUSE_UPDATE = "HOUSE_UPDATE";
    public static final String HOUSE_DELETE = "HOUSE_DELETE";
    public static final String HOUSE_MANAGE = "HOUSE_MANAGE";

    // ---- ROOM ----
    public static final String ROOM_VIEW = "ROOM_VIEW";
    public static final String ROOM_CREATE = "ROOM_CREATE";
    public static final String ROOM_UPDATE = "ROOM_UPDATE";
    public static final String ROOM_DELETE = "ROOM_DELETE";
    public static final String ROOM_MANAGE = "ROOM_MANAGE";

    // ---- TENANT ----
    public static final String TENANT_VIEW = "TENANT_VIEW";
    public static final String TENANT_CREATE = "TENANT_CREATE";
    public static final String TENANT_UPDATE = "TENANT_UPDATE";
    public static final String TENANT_DELETE = "TENANT_DELETE";
    public static final String TENANT_MANAGE = "TENANT_MANAGE";

    // ---- CONTRACT ----
    public static final String CONTRACT_VIEW = "CONTRACT_VIEW";
    public static final String CONTRACT_CREATE = "CONTRACT_CREATE";
    public static final String CONTRACT_UPDATE = "CONTRACT_UPDATE";
    public static final String CONTRACT_DELETE = "CONTRACT_DELETE";
    public static final String CONTRACT_MANAGE = "CONTRACT_MANAGE";

    // ---- PAYMENT ----
    public static final String PAYMENT_VIEW = "PAYMENT_VIEW";
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String PAYMENT_MANAGE = "PAYMENT_MANAGE";

    // ---- INVOICE ----
    public static final String INVOICE_VIEW = "INVOICE_VIEW";
    public static final String INVOICE_CREATE = "INVOICE_CREATE";
    public static final String INVOICE_UPDATE = "INVOICE_UPDATE";
    public static final String INVOICE_MANAGE = "INVOICE_MANAGE";

    // ---- MAINTENANCE ----
    public static final String MAINTENANCE_VIEW = "MAINTENANCE_VIEW";
    public static final String MAINTENANCE_CREATE = "MAINTENANCE_CREATE";
    public static final String MAINTENANCE_UPDATE = "MAINTENANCE_UPDATE";
    public static final String MAINTENANCE_DELETE = "MAINTENANCE_DELETE";
    public static final String MAINTENANCE_MANAGE = "MAINTENANCE_MANAGE";

    // ---- EXPENSE ----
    public static final String EXPENSE_VIEW = "EXPENSE_VIEW";
    public static final String EXPENSE_CREATE = "EXPENSE_CREATE";
    public static final String EXPENSE_UPDATE = "EXPENSE_UPDATE";
    public static final String EXPENSE_DELETE = "EXPENSE_DELETE";
    public static final String EXPENSE_MANAGE = "EXPENSE_MANAGE";

    // ---- AMENITY ----
    public static final String AMENITY_VIEW = "AMENITY_VIEW";
    public static final String AMENITY_CREATE = "AMENITY_CREATE";
    public static final String AMENITY_UPDATE = "AMENITY_UPDATE";
    public static final String AMENITY_DELETE = "AMENITY_DELETE";
    public static final String AMENITY_MANAGE = "AMENITY_MANAGE";

    // ---- UTILITY ----
    public static final String UTILITY_VIEW = "UTILITY_VIEW";
    public static final String UTILITY_CREATE = "UTILITY_CREATE";
    public static final String UTILITY_UPDATE = "UTILITY_UPDATE";
    public static final String UTILITY_MANAGE = "UTILITY_MANAGE";

    // ---- USER ----
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_MANAGE = "USER_MANAGE";

    // ---- REPORT ----
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String REPORT_MANAGE = "REPORT_MANAGE";

    // ---- DASHBOARD ----
    public static final String DASHBOARD_VIEW = "DASHBOARD_VIEW";

    // ---- SETTING ----
    public static final String SETTING_VIEW = "SETTING_VIEW";
    public static final String SETTING_UPDATE = "SETTING_UPDATE";
    public static final String SETTING_MANAGE = "SETTING_MANAGE";

    // ---- ASSISTANT ----
    public static final String ASSISTANT_VIEW = "ASSISTANT_VIEW";
    public static final String ASSISTANT_MANAGE = "ASSISTANT_MANAGE";

    // ---- NOTIFICATION ----
    public static final String NOTIFICATION_VIEW = "NOTIFICATION_VIEW";
    public static final String NOTIFICATION_MANAGE = "NOTIFICATION_MANAGE";

    // ---- HOUSE ACCESS ----
    public static final String HOUSE_ACCESS_VIEW = "HOUSE_ACCESS_VIEW";
    public static final String HOUSE_ACCESS_MANAGE = "HOUSE_ACCESS_MANAGE";

    // ---- LISTING ----
    public static final String LISTING_VIEW = "LISTING_VIEW";
    public static final String LISTING_MANAGE = "LISTING_MANAGE";
    public static final String LISTING_APPROVE = "LISTING_APPROVE";

    // ---- FEEDBACK ----
    public static final String FEEDBACK_CREATE = "FEEDBACK_CREATE";
    public static final String FEEDBACK_VIEW = "FEEDBACK_VIEW";
    public static final String FEEDBACK_MANAGE = "FEEDBACK_MANAGE";
}
