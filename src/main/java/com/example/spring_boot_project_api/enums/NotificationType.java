package com.example.spring_boot_project_api.enums;

/**
 * The kind of business event a notification represents. Kept as a VARCHAR so new
 * event types can be added without schema changes.
 */
public enum NotificationType {
    PROPERTY_SUBMITTED,
    INVOICE_CREATED,
    PAYMENT_CREATED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    MAINTENANCE_REQUEST,
    MAINTENANCE_STATUS,
    CONTRACT_EXPIRING,
    ROOM_ASSIGNED
}