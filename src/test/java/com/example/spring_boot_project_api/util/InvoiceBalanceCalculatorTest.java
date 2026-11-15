package com.example.spring_boot_project_api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

class InvoiceBalanceCalculatorTest {

    private static final BigDecimal TOTAL = new BigDecimal("193.04");

    @Test
    void unpaidInvoiceKeepsItsFullBalanceAndIsPending() {
        assertThat(InvoiceBalanceCalculator.invoiceTotal(
                new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("3.04")))
                .isEqualByComparingTo(TOTAL);
        assertThat(InvoiceBalanceCalculator.remainingBalance(TOTAL, BigDecimal.ZERO))
                .isEqualByComparingTo("193.04");
        assertThat(InvoiceBalanceCalculator.paymentStatus(TOTAL, BigDecimal.ZERO))
                .isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void partialPaymentKeepsOriginalTotalAndCalculatesRemainingBalance() {
        BigDecimal paid = new BigDecimal("100.00");

        assertThat(InvoiceBalanceCalculator.remainingBalance(TOTAL, paid)).isEqualByComparingTo("93.04");
        assertThat(InvoiceBalanceCalculator.paymentStatus(TOTAL, paid)).isEqualTo(InvoiceStatus.PARTIAL);
    }

    @Test
    void exactPaymentHasNoRemainingBalanceAndIsPaid() {
        assertThat(InvoiceBalanceCalculator.remainingBalance(TOTAL, TOTAL)).isZero();
        assertThat(InvoiceBalanceCalculator.paymentStatus(TOTAL, TOTAL)).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void multipleSuccessfulPaymentsAreAppliedToTheSameOriginalTotal() {
        BigDecimal paid = new BigDecimal("100.00").add(new BigDecimal("50.00"));

        assertThat(InvoiceBalanceCalculator.remainingBalance(TOTAL, paid)).isEqualByComparingTo("43.04");
        assertThat(InvoiceBalanceCalculator.paymentStatus(TOTAL, paid)).isEqualTo(InvoiceStatus.PARTIAL);
    }

    @Test
    void overpaymentNeverProducesANegativeRemainingBalance() {
        assertThat(InvoiceBalanceCalculator.remainingBalance(TOTAL, new BigDecimal("250.00"))).isZero();
        assertThat(InvoiceBalanceCalculator.paymentStatus(TOTAL, new BigDecimal("250.00")))
                .isEqualTo(InvoiceStatus.PAID);
    }
}
