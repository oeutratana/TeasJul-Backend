package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateOwnerSettingsRequest(

        // General Settings
        @NotBlank(message = "Base currency is required")
        String baseCurrency,

        @NotBlank(message = "Display currency is required")
        @Pattern(regexp = "USD|KHR|BOTH", message = "Display currency must be USD, KHR, or BOTH")
        String displayCurrency,

        @NotNull(message = "Exchange rate is required")
        @DecimalMin(value = "0", message = "Exchange rate must be positive")
        java.math.BigDecimal exchangeRate,

        @NotBlank(message = "Date format is required")
        String dateFormat,

        @NotBlank(message = "Timezone is required")
        String timezone,

        // Branding Settings
        String businessName,
        String logoUrl,
        String phone,
        String email,
        String address,
        String primaryColor,
        String secondaryColor,

        // Document Language
        @NotBlank(message = "Document language is required")
        @Pattern(regexp = "KM|EN|BOTH", message = "Document language must be KM, EN, or BOTH")
        String documentLanguage,

        // Invoice Settings
        String invoicePrefix,
        Integer invoiceDefaultDueDays,
        String invoiceFooter,
        Boolean invoiceShowUsd,
        Boolean invoiceShowKhr,
        Boolean invoiceShowLogo,
        Boolean invoiceShowBusinessInfo,

        // Contract Settings
        String contractTitle,
        String contractPaymentTerms,
        String contractDepositTerms,
        String contractTerminationTerms,
        String contractAdditionalTerms,
        @Pattern(regexp = "KM|EN|BOTH", message = "Contract language must be KM, EN, or BOTH")
        String contractLanguage,

        // Contract Party A (the owner's own identity on rental agreements)
        @Valid
        UpdateOwnerContractInfoRequest contractParty,

        // Receipt Settings
        String receiptPrefix,
        @Pattern(regexp = "KM|EN|BOTH", message = "Receipt language must be KM, EN, or BOTH")
        String receiptLanguage,
        String receiptFooter,
        Boolean receiptShowPaymentMethod,
        Boolean receiptShowTransactionId,
        Boolean receiptShowUsd,
        Boolean receiptShowKhr,

        // Telegram Settings
        String telegramBotToken,
        String telegramChatId
) {
}
