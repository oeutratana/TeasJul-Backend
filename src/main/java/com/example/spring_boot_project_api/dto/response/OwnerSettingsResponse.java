package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerSettingsResponse(
        Long id,
        Long ownerId,

        // General
        String baseCurrency,
        String displayCurrency,
        BigDecimal exchangeRate,
        String dateFormat,
        String timezone,

        // Branding
        String businessName,
        String logoUrl,
        String phone,
        String email,
        String address,
        String primaryColor,
        String secondaryColor,

        // Document Language
        String documentLanguage,

        // Invoice
        String invoicePrefix,
        Integer invoiceDefaultDueDays,
        String invoiceFooter,
        Boolean invoiceShowUsd,
        Boolean invoiceShowKhr,
        Boolean invoiceShowLogo,
        Boolean invoiceShowBusinessInfo,

        // Contract
        String contractTitle,
        String contractPaymentTerms,
        String contractDepositTerms,
        String contractTerminationTerms,
        String contractAdditionalTerms,
        String contractLanguage,
        OwnerContractInfoResponse contractParty,

        // Receipt
        String receiptPrefix,
        String receiptLanguage,
        String receiptFooter,
        Boolean receiptShowPaymentMethod,
        Boolean receiptShowTransactionId,
        Boolean receiptShowUsd,
        Boolean receiptShowKhr,

        // Telegram
        String telegramBotToken,
        String telegramChatId,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
