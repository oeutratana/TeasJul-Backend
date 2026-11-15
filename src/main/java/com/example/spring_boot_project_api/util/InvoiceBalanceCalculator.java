package com.example.spring_boot_project_api.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

/**
 * Canonical invoice payment arithmetic. {@code totalAmount} is the persisted
 * result of {@code subtotal - discount + lateFee}; it must never be replaced
 * with the remaining balance.
 */
public final class InvoiceBalanceCalculator {

    /** Default late-fee policy: a flat amount per calendar day past the due date. */
    public static final BigDecimal DAILY_LATE_FEE_RATE = new BigDecimal("1.00");

    private InvoiceBalanceCalculator() {
    }

    public static BigDecimal invoiceTotal(BigDecimal subtotal, BigDecimal discount, BigDecimal lateFee) {
        return amount(subtotal).subtract(amount(discount)).add(amount(lateFee));
    }

    public static BigDecimal remainingBalance(BigDecimal invoiceTotal, BigDecimal paidAmount) {
        return amount(invoiceTotal).subtract(amount(paidAmount)).max(BigDecimal.ZERO);
    }

    /**
     * Calendar days elapsed past {@code dueDate}. The due date itself is day 0 and a
     * future due date is never negative, so the result is always {@code >= 0}.
     */
    public static long overdueDays(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null || asOf == null) {
            return 0L;
        }
        return Math.max(ChronoUnit.DAYS.between(dueDate, asOf), 0L);
    }

    /**
     * Late fee implied by the policy for the given dates. Always derived from the due
     * date rather than from the previously stored fee, so recalculating on the same day
     * yields the same amount instead of compounding.
     */
    public static BigDecimal defaultLateFee(LocalDate dueDate, LocalDate asOf) {
        return DAILY_LATE_FEE_RATE.multiply(BigDecimal.valueOf(overdueDays(dueDate, asOf)));
    }

    /**
     * A late fee may only accrue while the invoice is still collectable. A fully paid
     * invoice stays paid with a zero remaining balance, and a cancelled invoice is closed.
     */
    public static boolean isLateFeeEligible(InvoiceStatus status) {
        return status != InvoiceStatus.PAID && status != InvoiceStatus.CANCELLED;
    }

    /**
     * An invoice is overdue when its due date has passed and money is still outstanding.
     * Comparison is by calendar date, not timestamp, and the due date itself is not late.
     *
     * <p>This is derived rather than read from {@link InvoiceStatus#OVERDUE}: the invoice
     * lifecycle never persists that status, so any status-based filter reports zero.
     */
    public static boolean isOverdue(LocalDate dueDate, BigDecimal remainingAmount, LocalDate asOf) {
        if (dueDate == null || asOf == null) {
            return false;
        }
        return dueDate.isBefore(asOf) && amount(remainingAmount).signum() > 0;
    }

    /** Maps the project's unpaid state to {@link InvoiceStatus#PENDING}. */
    public static InvoiceStatus paymentStatus(BigDecimal invoiceTotal, BigDecimal paidAmount) {
        BigDecimal total = amount(invoiceTotal);
        BigDecimal paid = amount(paidAmount);
        if (paid.signum() <= 0) {
            return InvoiceStatus.PENDING;
        }
        return paid.compareTo(total) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL;
    }

    public static BigDecimal amount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
