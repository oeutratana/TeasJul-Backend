package com.example.spring_boot_project_api.service;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.Payment;

public interface TelegramService {

    /**
     * Sends a Telegram message when a payment is initiated (e.g. a tenant
     * starts an online payment). Best-effort: failures are logged and never
     * propagated to the caller.
     */
    void sendPaymentCreated(Payment payment);

    /**
     * Sends a Telegram message when a payment is successfully paid (manual
     * recording or ABA-confirmed online payment). Best-effort: failures are
     * logged and never propagated to the caller.
     */
    void sendPaymentSuccess(Payment payment);

    /**
     * Sends a Telegram message to the SUPER_ADMIN destination when an owner
     * submits a property for approval. Reuses the existing
     * {@code PROPERTY_SUBMITTED} event hook, the Telegram channel switch and the
     * per-event notification setting.
     *
     * <p>Best-effort like the payment hooks: failures are logged (never the bot
     * token) and never propagated, so a property submission is never rolled back
     * by a Telegram outage.</p>
     */
    void sendPropertySubmittedToSuperAdmin(PropertySubmission submission);

    /**
     * Details of a submitted property, resolved by the notification service so
     * this service stays free of extra lookups.
     *
     * @param reference   human readable reference, e.g. {@code ROOM#77}
     * @param propertyType submitted entity type, e.g. {@code ROOM} or {@code PROPERTY}
     * @param propertyName property/house name shown to the super admin
     * @param ownerName   submitting owner
     * @param status      publication status after submission
     * @param submittedAt submission timestamp
     */
    record PropertySubmission(
            String reference,
            String propertyType,
            String propertyName,
            String ownerName,
            String status,
            LocalDateTime submittedAt
    ) {
    }
}